/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.mojang

import dev.shadow.eclipselauncher.core.common.AppResult
import dev.shadow.eclipselauncher.core.common.Failure
import dev.shadow.eclipselauncher.core.common.FailureStage
import dev.shadow.eclipselauncher.core.common.UserAction

/** A Java library that belongs on the game classpath. */
data class ResolvedLibrary(
    val coordinate: MavenCoordinate,
    val download: DownloadInfo,
    val path: String,
)

/**
 * A Mojang native artifact that is intentionally not downloaded. Android
 * cannot load any of them; EclipseNatives supplies the replacement set.
 */
data class DroppedNative(
    val coordinate: MavenCoordinate,
    val classifier: String,
    val download: DownloadInfo?,
    val reason: String,
)

/** Everything an installer needs from a version JSON before it fetches bytes. */
data class ResolvedVersion(
    val id: String,
    val mainClass: String,
    val assetIndex: AssetIndexRef,
    val client: DownloadInfo,
    val libraries: List<ResolvedLibrary>,
    val droppedNatives: List<DroppedNative>,
    val logging: LoggingClient?,
    val javaMajorVersion: Int?,
    val arguments: Arguments?,
    val minecraftArguments: String?,
)

/**
 * Turns a parsed version JSON into a download plan for one launch context.
 *
 * This is pure metadata work: no network, no disk, no Android type. It is the
 * same code the installer and the launch planner use, so the classpath cannot
 * drift between install time and launch time.
 */
object VersionResolver {
    private const val NATIVE_REASON =
        "Android loads the EclipseNatives build instead of Mojang's desktop native"

    fun resolve(version: VersionJson, context: LaunchContext): AppResult<ResolvedVersion> =
        try {
            AppResult.Success(resolveOrThrow(version, context))
        } catch (error: MetadataException) {
            AppResult.Error(
                Failure(
                    code = "MOJANG_METADATA_INVALID",
                    stage = FailureStage.RESOLVE,
                    retryable = false,
                    userMessage = "This version's metadata is not usable.",
                    userAction = UserAction.CHOOSE_VERSION,
                    technicalDetail = error.message,
                ),
            )
        } catch (error: IllegalArgumentException) {
            AppResult.Error(
                Failure(
                    code = "MOJANG_METADATA_INVALID",
                    stage = FailureStage.RESOLVE,
                    retryable = false,
                    userMessage = "This version's metadata is not usable.",
                    userAction = UserAction.CHOOSE_VERSION,
                    technicalDetail = error.message,
                ),
            )
        }

    fun resolveOrThrow(version: VersionJson, context: LaunchContext): ResolvedVersion {
        val client = version.client
            ?: throw MetadataException("version ${version.id} has no client download")
        requireDownload(client, "client")

        val assetIndex = version.assetIndex
            ?: throw MetadataException("version ${version.id} has no asset index")
        requireDownload(assetIndex.url, assetIndex.sha1, assetIndex.size, "asset index")

        val logging = version.logging?.client
        if (logging != null) {
            requireDownload(logging.file, "logging config")
        }

        val classpath = LinkedHashMap<String, ResolvedLibrary>()
        val natives = mutableListOf<DroppedNative>()

        for (library in version.libraries) {
            if (!RuleEvaluator.allows(library.rules, context, default = true)) continue

            val coordinate = MavenCoordinate.parse(library.name)
            val nativeClassifier = nativeClassifier(library, context)
            if (nativeClassifier != null) {
                val classifier = nativeClassifier.replace("\${arch}", context.osArch)
                if (classifier.contains("\${")) {
                    throw MetadataException(
                        "native classifier for ${library.name} has an unknown macro: $nativeClassifier",
                    )
                }
                natives += DroppedNative(
                    coordinate = coordinate,
                    classifier = classifier,
                    download = library.downloads.classifiers[classifier],
                    reason = NATIVE_REASON,
                )
                continue
            }

            val artifact = library.downloads.artifact
                ?: throw MetadataException("library ${library.name} has no artifact")
            requireDownload(artifact, "library ${library.name}")
            val path = safeRelativePath(artifact.path ?: coordinate.relativePath)
            classpath[coordinate.moduleKey] = ResolvedLibrary(
                coordinate = coordinate,
                download = artifact,
                path = path,
            )
        }

        return ResolvedVersion(
            id = version.id,
            mainClass = version.mainClass,
            assetIndex = assetIndex,
            client = client,
            libraries = classpath.values.toList(),
            droppedNatives = natives,
            logging = logging,
            javaMajorVersion = version.javaVersion?.majorVersion,
            arguments = version.arguments,
            minecraftArguments = version.minecraftArguments,
        )
    }

    private fun nativeClassifier(library: Library, context: LaunchContext): String? {
        if (library.natives.isEmpty()) return null
        val osKey = when (context.osName.lowercase()) {
            "osx", "macos", "mac" -> "osx"
            else -> context.osName.lowercase()
        }
        return library.natives[osKey]
    }

    private fun requireDownload(download: DownloadInfo, what: String) {
        requireDownload(
            url = download.url,
            sha1 = download.sha1,
            size = download.size,
            what = what,
        )
    }

    private fun requireDownload(url: String?, sha1: String?, size: Long?, what: String) {
        if (url.isNullOrBlank()) throw MetadataException("$what has no download URL")
        if (sha1.isNullOrBlank()) throw MetadataException("$what has no SHA-1")
        if (size == null || size < 0) throw MetadataException("$what has no size")
    }

    /**
     * A download path is joined to the shared store. It must be relative and
     * must not contain a traversal segment; the URL itself is checked by the
     * network layer, not here.
     */
    fun safeRelativePath(path: String): String {
        if (path.isBlank()) throw MetadataException("empty download path")
        if (path.startsWith('/') || path.startsWith('\\')) {
            throw MetadataException("absolute download path: $path")
        }
        if (path.any { it == '\\' || it == '\u0000' }) {
            throw MetadataException("unsafe download path: $path")
        }
        val segments = path.split('/')
        if (segments.any { it.isEmpty() || it == "." || it == ".." }) {
            throw MetadataException("unsafe download path: $path")
        }
        return path
    }
}

/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.install

import dev.shadow.eclipselauncher.core.common.AppResult
import dev.shadow.eclipselauncher.core.common.Failure
import dev.shadow.eclipselauncher.core.common.FailureStage
import dev.shadow.eclipselauncher.core.common.UserAction
import dev.shadow.eclipselauncher.domain.mojang.AssetIndex
import dev.shadow.eclipselauncher.domain.mojang.MetadataException
import dev.shadow.eclipselauncher.domain.mojang.ResolvedVersion
import dev.shadow.eclipselauncher.domain.mojang.VersionResolver
import java.net.URI
import java.net.URISyntaxException

/** The role of a download, used for progress copy and repair policy. */
enum class DownloadKind {
    CLIENT,
    LIBRARY,
    ASSET_INDEX,
    ASSET,
    LOGGING,
}

/** One verified byte range in the shared store. */
data class DownloadItem(
    val kind: DownloadKind,
    val url: String,
    val sha1: String,
    val size: Long,
    val destination: String,
)

/**
 * The complete, deterministic download list for one version.
 *
 * Paths are relative to the shared store root. The installer joins them to
 * app-specific storage; a plan never contains an absolute path, so the same
 * plan can be moved between devices without editing.
 */
data class InstallPlan(
    val versionId: String,
    val downloads: List<DownloadItem>,
    val client: DownloadItem,
    val assetIndex: DownloadItem,
    val libraries: List<DownloadItem>,
    val assets: List<DownloadItem>,
    val logging: DownloadItem?,
    val totalBytes: Long,
)

/**
 * The shared-store layout from doc 07 §6. Directories are relative and are
 * validated before a path is built.
 */
data class InstallLayout(
    val versionsDir: String = "versions",
    val librariesDir: String = "libraries",
    val assetsDir: String = "assets",
    val loggingDir: String = "logging",
) {
    init {
        VersionResolver.safeRelativePath(versionsDir)
        VersionResolver.safeRelativePath(librariesDir)
        VersionResolver.safeRelativePath(assetsDir)
        VersionResolver.safeRelativePath(loggingDir)
    }

    fun clientPath(versionId: String): String = join(versionsDir, safeSegment(versionId), "client.jar")

    fun libraryPath(relativePath: String): String = join(librariesDir, relativePath)

    fun assetIndexPath(indexId: String): String =
        join(assetsDir, "indexes", safeSegment(indexId) + ".json")

    fun assetObjectPath(hash: String): String {
        if (!SHA1.matches(hash)) throw MetadataException("asset hash is not a SHA-1: $hash")
        return join(assetsDir, "objects", hash.substring(0, 2), hash)
    }

    fun loggingPath(sha1: String): String {
        if (!SHA1.matches(sha1)) throw MetadataException("logging hash is not a SHA-1: $sha1")
        return join(loggingDir, "$sha1.xml")
    }

    private fun join(vararg parts: String): String =
        VersionResolver.safeRelativePath(parts.joinToString("/"))

    private fun safeSegment(value: String): String {
        if (value.contains('/') || value.contains('\\')) {
            throw MetadataException("expected a single path segment: $value")
        }
        VersionResolver.safeRelativePath(value)
        return value
    }

    private companion object {
        val SHA1 = Regex("[0-9a-f]{40}")
    }
}

/**
 * Builds the download list from resolved version metadata and a parsed asset
 * index. No I/O: the caller can inspect the size, persist the plan and hand it
 * to the downloader without re-reading Mojang metadata.
 */
object InstallPlanBuilder {
    fun build(
        version: ResolvedVersion,
        assetIndex: AssetIndex,
        layout: InstallLayout = InstallLayout(),
    ): AppResult<InstallPlan> = try {
        AppResult.Success(buildOrThrow(version, assetIndex, layout))
    } catch (error: MetadataException) {
        failure(error)
    } catch (error: IllegalArgumentException) {
        failure(error)
    }

    fun buildOrThrow(
        version: ResolvedVersion,
        assetIndex: AssetIndex,
        layout: InstallLayout = InstallLayout(),
    ): InstallPlan {
        val client = item(
            kind = DownloadKind.CLIENT,
            url = version.client.url,
            sha1 = version.client.sha1,
            size = version.client.size,
            destination = layout.clientPath(version.id),
        )

        val libraries = version.libraries.map { library ->
            item(
                kind = DownloadKind.LIBRARY,
                url = library.download.url,
                sha1 = library.download.sha1,
                size = library.download.size,
                destination = layout.libraryPath(library.path),
            )
        }

        val index = item(
            kind = DownloadKind.ASSET_INDEX,
            url = version.assetIndex.url,
            sha1 = version.assetIndex.sha1,
            size = version.assetIndex.size,
            destination = layout.assetIndexPath(version.assetIndex.id),
        )

        val logging = version.logging?.file?.let { file ->
            val digest = file.sha1
                ?: throw MetadataException("logging config has no SHA-1")
            item(
                kind = DownloadKind.LOGGING,
                url = file.url,
                sha1 = digest,
                size = file.size,
                destination = layout.loggingPath(digest),
            )
        }

        val assets = assetIndex.objects.values
            .distinctBy { it.hash }
            .map { asset ->
                item(
                    kind = DownloadKind.ASSET,
                    url = assetUrl(asset.hash),
                    sha1 = asset.hash,
                    size = asset.size,
                    destination = layout.assetObjectPath(asset.hash),
                )
            }
            .sortedBy { it.destination }

        val all = buildList {
            add(client)
            addAll(libraries)
            add(index)
            if (logging != null) add(logging)
            addAll(assets)
        }
        ensureUniqueDestinations(all)

        return InstallPlan(
            versionId = version.id,
            downloads = all,
            client = client,
            assetIndex = index,
            libraries = libraries,
            assets = assets,
            logging = logging,
            totalBytes = all.sumOf { it.size },
        )
    }

    private fun item(
        kind: DownloadKind,
        url: String?,
        sha1: String?,
        size: Long?,
        destination: String,
    ): DownloadItem {
        val actualUrl = url
            ?: throw MetadataException("$kind has no URL")
        if (actualUrl.isBlank()) throw MetadataException("$kind has no URL")
        requireHttps(actualUrl, kind)
        val digest = sha1
            ?: throw MetadataException("$kind has no SHA-1")
        if (!SHA1.matches(digest)) {
            throw MetadataException("$kind has an invalid SHA-1: $digest")
        }
        val actualSize = size
            ?: throw MetadataException("$kind has no size")
        if (actualSize < 0) throw MetadataException("$kind has no size")
        VersionResolver.safeRelativePath(destination)
        return DownloadItem(
            kind = kind,
            url = actualUrl,
            sha1 = digest,
            size = actualSize,
            destination = destination,
        )
    }

    /**
     * The full host allowlist lives in the network layer, because it has to
     * re-check every redirect. This guard is the cheaper first line: a plan
     * never contains cleartext, credentials in the authority, or a non-443
     * port, so a broken metadata entry fails before a single byte is fetched.
     */
    private fun requireHttps(url: String, kind: DownloadKind) {
        val uri = try {
            URI(url)
        } catch (error: URISyntaxException) {
            throw MetadataException("$kind has an invalid URL: ${error.message}")
        }
        if (!uri.scheme.equals("https", ignoreCase = true)) {
            throw MetadataException("$kind is not HTTPS: $url")
        }
        if (uri.host.isNullOrBlank()) {
            throw MetadataException("$kind has no host: $url")
        }
        if (uri.userInfo != null) {
            throw MetadataException("$kind URL contains credentials: $url")
        }
        if (uri.port != -1 && uri.port != 443) {
            throw MetadataException("$kind uses a non-443 port: $url")
        }
    }

    private fun ensureUniqueDestinations(items: List<DownloadItem>) {
        val byDestination = HashMap<String, DownloadItem>()
        for (item in items) {
            val previous = byDestination.put(item.destination, item)
            if (previous != null &&
                (previous.sha1 != item.sha1 || previous.size != item.size)
            ) {
                throw MetadataException(
                    "two downloads share ${item.destination}: " +
                        "${previous.sha1} and ${item.sha1}",
                )
            }
        }
    }

    private fun assetUrl(hash: String): String =
        "https://resources.download.minecraft.net/${hash.substring(0, 2)}/$hash"

    private fun failure(error: Exception): AppResult.Error = AppResult.Error(
        Failure(
            code = "INSTALL_PLAN_INVALID",
            stage = FailureStage.INSTALL,
            retryable = false,
            userMessage = "The install plan could not be built.",
            userAction = UserAction.NONE,
            technicalDetail = error.message,
        ),
    )

    private val SHA1 = Regex("[0-9a-f]{40}")
}

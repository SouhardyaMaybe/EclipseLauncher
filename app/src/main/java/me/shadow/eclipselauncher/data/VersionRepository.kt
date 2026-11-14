/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Version list management: the official piston-meta manifest flattened into
 * one date-ordered list (no latest-release / latest-snapshot aliases), the
 * mod-loader version lists (Fabric and Quilt JSON meta; Forge and NeoForge
 * maven metadata), and the per-loader availability lookup the loader picker
 * runs for a chosen Minecraft version (Fabric/Quilt game lists, OptiFine's
 * downloads page). Small documents, so a hard cap keeps a hostile response
 * from exhausting the heap; successful fetches are cached on disk and reused
 * when the network is down.
 */
package me.shadow.eclipselauncher.data

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/** Result of a metadata fetch: either a value or a user-facing error. */
sealed interface MetaResult<out T> {
    data class Success<T>(val value: T) : MetaResult<T>
    data class Error(val message: String) : MetaResult<Nothing>
}

@Serializable
data class ManifestVersion(
    val id: String,
    val type: String = "release",
    val releaseTime: String = "",
)

/**
 * What the loader picker needs for one Minecraft version: which of the six
 * loaders have a build for it, and each loader's own version list already
 * narrowed to it.
 */
data class LoaderPickerData(
    val available: Map<LoaderKind, Boolean>,
    val versions: Map<LoaderKind, MetaResult<List<VersionOption>>>,
)

@Serializable
data class VersionManifest(
    val latest: Map<String, String> = emptyMap(),
    val versions: List<ManifestVersion> = emptyList(),
)

@Serializable
data class FabricLoaderEntry(
    val version: String,
    val stable: Boolean = false,
)

/** One row of a loader meta's supported-game list (Fabric, Quilt). */
@Serializable
data class GameVersionEntry(
    val version: String = "",
)

/** On-disk-cached metadata client for the version selector's lists. */
class VersionRepository(
    private val cacheDir: File,
    private val network: NetworkClient,
) {
    private val json = network.json
    private var manifestCache: VersionManifest? = null

    /**
     * Fetches (or serves from cache) the official piston-meta manifest and
     * flattens it into ONE date-ordered list, newest first. The manifest's
     * `latest_release` / `latest_snapshot` pointers are deliberately not
     * offered as rows: they are aliases for another row rather than versions
     * of their own, and the old EclipseLauncher stored them raw, which broke
     * installs. This repository never hands out an alias.
     */
    suspend fun minecraftVersions(): MetaResult<List<McVersion>> = withContext(Dispatchers.IO) {
        val manifest = when (val result = fetchManifest()) {
            is MetaResult.Error -> return@withContext result
            is MetaResult.Success -> result.value
        }
        val versions = manifest.versions.map { version ->
            McVersion(
                id = version.id,
                type = version.type,
                releaseTime = parseReleaseTime(version.releaseTime),
            )
        }
        // The manifest already lists newest-first; the sort only matters once
        // releaseTime parsed, and keeps that true if the order ever changes.
        return@withContext MetaResult.Success(versions.sortedByDescending { it.releaseTime })
    }

    /**
     * Returns cached versions immediately without network call.
     * Returns empty list if no cache exists.
     * Useful for instant UI display while fresh data loads.
     */
    fun getCachedVersions(): List<McVersion> {
        return manifestCache?.let { manifest ->
            manifest.versions.map { version ->
                McVersion(
                    id = version.id,
                    type = version.type,
                    releaseTime = parseReleaseTime(version.releaseTime),
                )
            }.sortedByDescending { it.releaseTime }
        } ?: emptyList()
    }

    private fun parseReleaseTime(raw: String): Long = VersionRepository.parseReleaseTime(raw)

    /**
     * Loader version list for [mcVersion]'s picker. Fabric and Quilt loaders
     * are Minecraft-agnostic — one loader build serves every version their
     * meta has mappings for — so their lists come through whole. Forge and
     * NeoForge stamp the Minecraft version onto every build (`1.21-51.0.33`,
     * `21.1.177`), so those two are narrowed to the builds that actually
     * belong to [mcVersion]. OptiFine lists are scraped from the downloads
     * page per MC version. Vanilla has no list: the version is the MC version.
     */
    suspend fun loaderVersions(
        loader: LoaderKind,
        mcVersion: String = "",
    ): MetaResult<List<VersionOption>> = withContext(Dispatchers.IO) {
        when (loader) {
            LoaderKind.VANILLA -> MetaResult.Success(emptyList())
            LoaderKind.OPTIFINE -> if (mcVersion.isBlank()) {
                MetaResult.Success(emptyList())
            } else {
                fetchOptiFineVersions(mcVersion)
            }
            LoaderKind.FABRIC -> fetchFabric(FABRIC_LOADER_URL, FABRIC_CACHE)
            LoaderKind.QUILT -> fetchFabric(QUILT_LOADER_URL, QUILT_CACHE)
            LoaderKind.FORGE -> fetchMaven(FORGE_META_URL, FORGE_CACHE)
                // The predicate is handed the id itself: `1.21-51.0.33`
                // belongs to 1.21, everything after the dash is the build.
                .filterOptions { id -> id.substringBefore('-') == mcVersion }
            LoaderKind.NEOFORGE -> fetchMaven(NEOFORGE_META_URL, NEOFORGE_CACHE)
                .filterOptions { id -> neoforgeMatches(id, mcVersion) }
        }
    }

    /**
     * Everything the loader picker asks for in one go, fetched in parallel:
     * the six availability flags plus each loader's list for [mcVersion].
     *
     * Availability fails OPEN. A lookup that cannot be reached shows the
     * loader as available rather than locking the user out of a choice that
     * is probably fine; only a successful lookup that genuinely finds nothing
     * marks it unavailable. The screen is reached only after the manifest
     * loaded, so its ids are already known on this path.
     */
    suspend fun loaderPickerData(mcVersion: String): LoaderPickerData = coroutineScope {
        val fabricGames = async { gameVersionSet(FABRIC_INTERMEDIARY_URL, FABRIC_INTERMEDIARY_CACHE) }
        val quiltGames = async { gameVersionSet(QUILT_GAME_URL, QUILT_GAME_CACHE) }
        val optifineGames = async { optifineGameVersions() }
        val fabric = async { loaderVersions(LoaderKind.FABRIC) }
        val quilt = async { loaderVersions(LoaderKind.QUILT) }
        val forge = async { loaderVersions(LoaderKind.FORGE, mcVersion) }
        val neoforge = async { loaderVersions(LoaderKind.NEOFORGE, mcVersion) }

        fun hasBuilds(list: MetaResult<List<VersionOption>>) = when (list) {
            is MetaResult.Error -> true
            is MetaResult.Success -> list.value.isNotEmpty()
        }

        fun covers(games: MetaResult<Set<String>>) = when (games) {
            is MetaResult.Error -> true
            is MetaResult.Success -> games.value.isEmpty() || mcVersion in games.value
        }

        // The manifest is only needed to resolve NeoForge's encoded Minecraft
        // version, which neoforgeMatches does from the same in-memory cache.
        val optifine = async { loaderVersions(LoaderKind.OPTIFINE, mcVersion) }

        LoaderPickerData(
            available = mapOf(
                LoaderKind.VANILLA to true,
                LoaderKind.OPTIFINE to covers(optifineGames.await()),
                LoaderKind.FABRIC to covers(fabricGames.await()),
                LoaderKind.FORGE to hasBuilds(forge.await()),
                LoaderKind.NEOFORGE to hasBuilds(neoforge.await()),
                LoaderKind.QUILT to covers(quiltGames.await()),
            ),
            versions = mapOf(
                LoaderKind.VANILLA to MetaResult.Success(emptyList()),
                LoaderKind.OPTIFINE to optifine.await(),
                LoaderKind.FABRIC to fabric.await(),
                LoaderKind.QUILT to quilt.await(),
                LoaderKind.FORGE to forge.await(),
                LoaderKind.NEOFORGE to neoforge.await(),
            ),
        )
    }

    /**
     * Keeps [keep] on the ids of a successful list; errors pass through.
     * The predicate is suspend because NeoForge's has to ask the manifest
     * which ids are real before it can judge a build.
     */
    private suspend fun MetaResult<List<VersionOption>>.filterOptions(
        keep: suspend (String) -> Boolean,
    ): MetaResult<List<VersionOption>> = when (this) {
        is MetaResult.Error -> this
        is MetaResult.Success -> MetaResult.Success(value.filter { keep(it.id) })
    }

    /**
     * Whether a NeoForge build is the one for [mcVersion]. NeoForge encodes
     * the Minecraft version in the head of its own version string, but the
     * spelling changed with Minecraft's: three components are the old
     * `21.1.177` → 1.21.1 form, four are the new `26.1.0.0` → 26.1 form.
     * Candidates are tried most-specific first and only against ids the
     * manifest really publishes, so `26.1.0.0` lands on 26.1 while
     * `21.0.167` falls through 1.21.0 (never published) onto 1.21.
     */
    private suspend fun neoforgeMatches(neoforgeVersion: String, mcVersion: String): Boolean {
        val head = neoforgeVersion.takeWhile { it.isDigit() || it == '.' }.trim('.')
        if (head.isEmpty()) return neoforgeVersion.startsWith(mcVersion)
        val parts = head.split('.').filter { it.isNotEmpty() }
        if (parts.size < 2) return neoforgeVersion.startsWith(mcVersion)
        val manifestIds = when (val manifest = fetchManifest()) {
            is MetaResult.Success -> manifest.value.versions.mapTo(mutableSetOf()) { it.id }
            is MetaResult.Error -> return neoforgeVersion.startsWith(mcVersion)
        }
        if (manifestIds.isEmpty()) return neoforgeVersion.startsWith(mcVersion)
        val candidates = buildList {
            if (parts.size >= 3) add("${parts[0]}.${parts[1]}.${parts[2]}")
            add("1.${parts[0]}.${parts[1]}")
            add("${parts[0]}.${parts[1]}")
            add("1.${parts[0]}")
            add(parts[0])
        }
        return candidates.any { it == mcVersion && it in manifestIds }
    }

    /**
     * Minecraft versions a loader's own meta says it has mappings for —
     * Fabric's intermediary list and Quilt's game list are exactly that.
     */
    private suspend fun gameVersionSet(url: String, cache: String): MetaResult<Set<String>> {
        val text = when (val result = fetchText(url, MAX_LOADER_BYTES, cache)) {
            is MetaResult.Error -> return result
            is MetaResult.Success -> result.value
        }
        val entries = try {
            json.decodeFromString<List<GameVersionEntry>>(text)
        } catch (_: Exception) {
            return MetaResult.Error("The supported-game list could not be read.")
        }
        val ids = entries.map { it.version }.filter { it.isNotBlank() }.toSet()
        return if (ids.isEmpty()) {
            MetaResult.Error("The supported-game list came back empty.")
        } else {
            MetaResult.Success(ids)
        }
    }

    /**
     * Minecraft versions OptiFine ships a build for, read off its downloads
     * page: every file name carries the Minecraft version right after the
     * `OptiFine_` prefix, so the set of those is the set it supports. There
     * is no API behind it — plain HTML and one regex pass. Names written
     * `1.8.0` are folded onto the published id `1.8`.
     */
    private suspend fun optifineGameVersions(): MetaResult<Set<String>> {
        val html = when (
            val result = fetchText(OPTIFINE_URL, MAX_OPTIFINE_BYTES, OPTIFINE_CACHE)
        ) {
            is MetaResult.Error -> return result
            is MetaResult.Success -> result.value
        }
        val ids = OPTIFINE_FILE_REGEX.findAll(html)
            .map { it.groupValues[1] }
            .filter { it.isNotBlank() }
            .flatMap { id -> sequenceOf(id, id.removeSuffix(".0")) }
            .toSet()
        return if (ids.isEmpty()) {
            MetaResult.Error("The OptiFine version list could not be read.")
        } else {
            MetaResult.Success(ids)
        }
    }

    /**
     * Fetches OptiFine builds for a specific Minecraft version from the
     * downloads page. File names follow the pattern:
     * `OptiFine_<mcVersion>_HD_U_<build>.jar` or `preview_OptiFine_<mcVersion>_HD_U_<build>.jar`
     * The build identifier (e.g., `U_K2_pre5`, `U_J9`) becomes the version option.
     */
    private suspend fun fetchOptiFineVersions(mcVersion: String): MetaResult<List<VersionOption>> {
        val html = when (
            val result = fetchText(OPTIFINE_URL, MAX_OPTIFINE_BYTES, OPTIFINE_CACHE)
        ) {
            is MetaResult.Error -> return result
            is MetaResult.Success -> result.value
        }

        // Pattern: OptiFine_1.21.11_HD_U_J9 or preview_OptiFine_26.2_HD_U_K2_pre5
        // Extract escaped mcVersion first; use raw string for regex to avoid $ conflicts
        val mc = Regex.escape(mcVersion)
        val pattern = Regex(
            "preview_OptiFine_${mc}_HD[^_]*_([^\\.\"]+)" +
            "|OptiFine_${mc}_HD[^_]*_([^\\.\"]+)"
        )
        val builds = pattern.findAll(html)
            .map { match -> match.groupValues[1] ?: match.groupValues[2] }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
            .sortedDescending()

        return if (builds.isEmpty()) {
            MetaResult.Error("No OptiFine builds found for Minecraft $mcVersion")
        } else {
            MetaResult.Success(builds.map { VersionOption(it, it) })
        }
    }

    private suspend fun fetchManifest(): MetaResult<VersionManifest> {
        manifestCache?.let { return MetaResult.Success(it) }
        val text = when (
            val result = fetchText(MANIFEST_URL, MAX_MANIFEST_BYTES, MANIFEST_CACHE)
        ) {
            is MetaResult.Error -> return result
            is MetaResult.Success -> result.value
        }
        val parsed = try {
            json.decodeFromString<VersionManifest>(text)
        } catch (_: Exception) {
            return MetaResult.Error("The version metadata could not be read.")
        }
        if (parsed.versions.isEmpty()) {
            return MetaResult.Error("The official version list came back empty.")
        }
        manifestCache = parsed
        return MetaResult.Success(parsed)
    }

    private suspend fun fetchFabric(url: String, cache: String): MetaResult<List<VersionOption>> {
        val text = when (val result = fetchText(url, MAX_LOADER_BYTES, cache)) {
            is MetaResult.Error -> return result
            is MetaResult.Success -> result.value
        }
        val entries = try {
            json.decodeFromString<List<FabricLoaderEntry>>(text)
        } catch (_: Exception) {
            return MetaResult.Error("The loader version list could not be read.")
        }
        val options = entries
            .sortedWith(compareByDescending<FabricLoaderEntry> { it.stable }.thenByDescending { it.version })
            .map { VersionOption(id = it.version, label = it.version + if (it.stable) "" else " (beta)") }
        return if (options.isEmpty()) {
            MetaResult.Error("The loader version list came back empty.")
        } else {
            MetaResult.Success(options)
        }
    }

    /** Parses `<version>` entries out of a maven-metadata.xml document. */
    internal fun parseMavenVersions(xml: String): List<String> =
        MAVEN_VERSION_REGEX.findAll(xml)
            .map { it.groupValues[1] }
            .filter { it.isNotBlank() }
            .toList()

    private suspend fun fetchMaven(url: String, cache: String): MetaResult<List<VersionOption>> {
        val xml = when (val result = fetchText(url, MAX_LOADER_BYTES, cache)) {
            is MetaResult.Error -> return result
            is MetaResult.Success -> result.value
        }
        val versions = parseMavenVersions(xml)
        return if (versions.isEmpty()) {
            MetaResult.Error("The loader version list came back empty.")
        } else {
            MetaResult.Success(versions.map { VersionOption(it, it) })
        }
    }

    // ------------------------------------------------------------- transport

    /**
     * Network-first fetch with an on-disk fallback: a successful response is
     * written to [cache] and reused when the server is unreachable.
     */
    private suspend fun fetchText(
        url: String,
        maxBytes: Int,
        cache: String,
    ): MetaResult<String> = withContext(Dispatchers.IO) {
        when (val result = download(url, maxBytes)) {
            is MetaResult.Success -> {
                writeCache(cache, result.value)
                result
            }
            is MetaResult.Error -> {
                val cached = readCache(cache)
                if (cached != null) MetaResult.Success(cached) else result
            }
        }
    }

    private suspend fun download(url: String, maxBytes: Int): MetaResult<String> {
        return when (val text = network.getText(url, maxBytes)) {
            null -> MetaResult.Error("Could not reach the version server.")
            else -> MetaResult.Success(text)
        }
    }

    private fun readCache(name: String): String? = runCatching {
        File(cacheDir, name).takeIf { it.exists() && it.length() in 1..MAX_CACHE_BYTES }?.readText()
    }.getOrNull()

    private fun writeCache(name: String, text: String) {
        runCatching {
            cacheDir.mkdirs()
            File(cacheDir, name).writeText(text)
        }
    }

    companion object {
        /** Thread-safe date parser using SimpleDateFormat pool (faster than OffsetDateTime). */
        private val dateFormat = ThreadLocal.withInitial {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }

        /** `2026-10-27T13:40:29+00:00` → epoch millis, 0 when unparseable. */
        internal fun parseReleaseTime(raw: String): Long = try {
            dateFormat.get().parse(raw).time
        } catch (_: Exception) {
            0L
        }

        private val MAVEN_VERSION_REGEX = Regex("<version>([^<]+)</version>")

        /** `OptiFine_1.21.11_HD_U_J9.jar` → `1.21.11`. */
        private val OPTIFINE_FILE_REGEX = Regex("OptiFine_([^_]+)_")

        private const val MANIFEST_URL =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
        private const val FABRIC_LOADER_URL = "https://meta.fabricmc.net/v2/versions/loader"
        private const val FABRIC_INTERMEDIARY_URL = "https://meta.fabricmc.net/v2/versions/intermediary"
        private const val QUILT_LOADER_URL = "https://meta.quiltmc.org/v3/versions/loader"
        private const val QUILT_GAME_URL = "https://meta.quiltmc.org/v3/versions/game"
        private const val FORGE_META_URL =
            "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml"
        private const val NEOFORGE_META_URL =
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"
        private const val OPTIFINE_URL = "https://optifine.net/downloads"

        private const val MANIFEST_CACHE = "version_manifest.json"
        private const val FABRIC_CACHE = "fabric_loaders.json"
        private const val FABRIC_INTERMEDIARY_CACHE = "fabric_intermediary.json"
        private const val QUILT_CACHE = "quilt_loaders.json"
        private const val QUILT_GAME_CACHE = "quilt_games.json"
        private const val FORGE_CACHE = "forge_loaders.xml"
        private const val NEOFORGE_CACHE = "neoforge_loaders.xml"
        private const val OPTIFINE_CACHE = "optifine_downloads.html"

        private const val MAX_MANIFEST_BYTES = 8 * 1024 * 1024
        private const val MAX_LOADER_BYTES = 4 * 1024 * 1024
        private const val MAX_OPTIFINE_BYTES = 4 * 1024 * 1024
        private const val MAX_CACHE_BYTES = 16 * 1024 * 1024
    }
}
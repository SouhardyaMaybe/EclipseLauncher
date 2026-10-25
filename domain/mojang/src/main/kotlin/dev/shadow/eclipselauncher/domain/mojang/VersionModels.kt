/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.mojang

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * The official Mojang metadata, modelled as data rather than as a downloaded
 * `Map<String, Any>`. The parser ignores fields we do not consume today so a
 * future Mojang addition cannot break an existing install.
 */
val MojangJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    isLenient = false
}

@Serializable
data class VersionManifest(
    val latest: LatestVersions,
    val versions: List<VersionSummary>,
)

@Serializable
data class LatestVersions(
    val release: String,
    val snapshot: String,
)

@Serializable
data class VersionSummary(
    val id: String,
    val type: String,
    val url: String,
    val sha1: String,
    val time: String? = null,
    val releaseTime: String? = null,
    val complianceLevel: Int? = null,
)

@Serializable
data class VersionJson(
    val id: String,
    val type: String = "release",
    val mainClass: String,
    val assets: String = "",
    val assetIndex: AssetIndexRef? = null,
    val downloads: Map<String, DownloadInfo> = emptyMap(),
    val libraries: List<Library> = emptyList(),
    val arguments: Arguments? = null,
    val minecraftArguments: String? = null,
    val logging: Logging? = null,
    val javaVersion: JavaVersion? = null,
    val complianceLevel: Int? = null,
    val minimumLauncherVersion: Int? = null,
) {
    /** The client jar is mandatory; every other download entry is optional. */
    val client: DownloadInfo?
        get() = downloads["client"]
}

@Serializable
data class AssetIndexRef(
    val id: String,
    val sha1: String,
    val size: Long,
    val totalSize: Long? = null,
    val url: String,
)

@Serializable
data class DownloadInfo(
    val url: String? = null,
    val sha1: String? = null,
    val size: Long? = null,
    val path: String? = null,
)

@Serializable
data class Library(
    val name: String,
    val downloads: LibraryDownloads = LibraryDownloads(),
    val natives: Map<String, String> = emptyMap(),
    val rules: List<Rule> = emptyList(),
    val extract: Extract? = null,
)

@Serializable
data class LibraryDownloads(
    val artifact: DownloadInfo? = null,
    val classifiers: Map<String, DownloadInfo> = emptyMap(),
)

@Serializable
data class Extract(
    val exclude: List<String> = emptyList(),
)

@Serializable
data class Rule(
    val action: String,
    val os: OsRule? = null,
    val features: Map<String, Boolean> = emptyMap(),
)

@Serializable
data class OsRule(
    val name: String? = null,
    val arch: String? = null,
    val version: String? = null,
)

@Serializable
data class Arguments(
    val game: List<JsonElement> = emptyList(),
    val jvm: List<JsonElement> = emptyList(),
)

@Serializable
data class Logging(
    val client: LoggingClient,
)

@Serializable
data class LoggingClient(
    val argument: String? = null,
    @SerialName("file") val file: DownloadInfo,
    val type: String,
)

@Serializable
data class JavaVersion(
    val component: String,
    val majorVersion: Int,
)

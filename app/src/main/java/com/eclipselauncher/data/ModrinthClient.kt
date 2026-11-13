/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Modrinth mod-market client: a thin wrapper over the public v2 search API
 * (`/v2/search` with a project_type:mod facet). Results are parsed with
 * kotlinx.serialization and only the fields the list needs are kept.
 */
package com.eclipselauncher.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ModrinthHit(
    val project_id: String = "",
    val slug: String = "",
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val downloads: Long = 0,
    val icon_url: String = "",
    val categories: List<String> = emptyList(),
    val versions: List<String> = emptyList(),
)

@Serializable
private data class ModrinthSearchResponse(
    val hits: List<ModrinthHit> = emptyList(),
    val totalHits: Int = 0,
)

class ModrinthClient {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Searches Modrinth projects; empty query returns the most relevant list.
     * [projectType] restricts the facet — "mod" for the market, "modpack" for
     * the pack browser. Modrinth is the only provider available without a key.
     */
    suspend fun searchMods(
        query: String,
        limit: Int = 25,
        projectType: String = "mod",
    ): MetaResult<List<ModrinthHit>> =
        withContext(Dispatchers.IO) {
            val encoded = URLEncoder.encode(query.trim(), Charsets.UTF_8.name())
            val facets = URLEncoder.encode(
                "[[\"project_type:$projectType\"]]",
                Charsets.UTF_8.name(),
            )
            val url = "$SEARCH_URL?query=$encoded&limit=$limit&facets=$facets"
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                connection.setRequestProperty("User-Agent", USER_AGENT)
                try {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        return@withContext MetaResult.Error("Could not reach Modrinth.")
                    }
                    val text = connection.inputStream
                        .use { it.readBytes() }
                        .toString(Charsets.UTF_8)
                    val parsed = json.decodeFromString<ModrinthSearchResponse>(text)
                    MetaResult.Success(parsed.hits)
                } finally {
                    connection.disconnect()
                }
            } catch (_: IOException) {
                MetaResult.Error("Network error while searching Modrinth.")
            } catch (_: Exception) {
                MetaResult.Error("The Modrinth response could not be read.")
            }
        }

    /** Canonical project page URL for a hit, used by the "open" action. */
    fun projectUrl(hit: ModrinthHit): String =
        "https://modrinth.com/mod/" + hit.slug.ifEmpty { hit.project_id }

    companion object {
        private const val SEARCH_URL = "https://api.modrinth.com/v2/search"
        private const val USER_AGENT = "EclipseLauncher/0.1 (github.com/SouhardyaMaybe/Eclipse)"
    }
}

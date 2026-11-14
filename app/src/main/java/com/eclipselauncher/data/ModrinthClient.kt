/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Modrinth mod-market client: a thin wrapper over the public v2 search API
 * (`/v2/search` with a project_type facet). Results are parsed with the
 * shared Ktor JSON pipeline and only the fields the list needs are kept.
 */
package com.eclipselauncher.data

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

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
data class ModrinthSearchResult(
    val hits: List<ModrinthHit>,
    val totalHits: Int,
    val offset: Int,
    val limit: Int,
    val totalPages: Int,
    val pageNumber: Int,
    val isLastPage: Boolean,
    /** True when the request failed at the network/transport layer. */
    val failed: Boolean = false,
)

/** One downloadable file attached to a Modrinth project version. */
@Serializable
data class ModrinthVersionFile(
    val url: String = "",
    val filename: String = "",
    val primary: Boolean = false,
    val size: Long = 0,
    val file_type: String? = null,
)

/** One published project version (mod, resource pack, shader, world...). */
@Serializable
data class ModrinthVersion(
    val id: String = "",
    val project_id: String = "",
    val name: String = "",
    val version_number: String = "",
    val game_versions: List<String> = emptyList(),
    val loaders: List<String> = emptyList(),
    val downloads: Long = 0,
    val date_published: String = "",
    val version_type: String = "release",
    val files: List<ModrinthVersionFile> = emptyList(),
)

/** Version list result; [failed] distinguishes transport errors from an empty list. */
data class ModrinthVersionsResult(
    val versions: List<ModrinthVersion> = emptyList(),
    val failed: Boolean = false,
)

@Serializable
private data class ModrinthSearchResponse(
    val hits: List<ModrinthHit> = emptyList(),
    val total_hits: Int = 0,
    val offset: Int = 0,
    val limit: Int = 0,
) {
    fun toResult(limit: Int, offset: Int): ModrinthSearchResult {
        val totalPages = (total_hits + limit - 1) / limit
        val pageNumber = offset / limit + 1
        val isLastPage = (offset + limit) >= total_hits
        return ModrinthSearchResult(
            hits = hits,
            totalHits = total_hits,
            offset = offset,
            limit = limit,
            totalPages = totalPages,
            pageNumber = pageNumber,
            isLastPage = isLastPage,
        )
    }
}

@Singleton
class ModrinthClient @Inject constructor(
    private val network: NetworkClient,
) {
    /**
     * Searches Modrinth projects; empty query returns the most relevant list.
     * [projectType] restricts the facet — "mod" for the market, "modpack" for
     * the pack browser. Modrinth is the only provider available without a key.
     */
    suspend fun searchMods(
        query: String,
        limit: Int = 25,
        offset: Int = 0,
        projectType: String = "mod",
    ): ModrinthSearchResult = withContext(Dispatchers.IO) {
        try {
            val parsed: ModrinthSearchResponse = network.http.get(SEARCH_URL) {
                parameter("query", query.trim())
                parameter("limit", limit)
                parameter("offset", offset)
                parameter("facets", "[[\"project_type:$projectType\"]]")
            }.body()
            parsed.toResult(limit, offset)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Return empty result on error; callers check hits.isEmpty()
            ModrinthSearchResult(
                hits = emptyList(),
                totalHits = 0,
                offset = 0,
                limit = limit,
                totalPages = 0,
                pageNumber = 1,
                isLastPage = true,
                failed = true,
            )
        }
    }

    /**
     * Lists every published version of one project, newest first. Changelogs
     * are excluded because the install sheet only needs version metadata.
     */
    suspend fun projectVersions(projectId: String): ModrinthVersionsResult =
        withContext(Dispatchers.IO) {
            try {
                val versions: List<ModrinthVersion> = network.http
                    .get("$PROJECT_URL/$projectId/version") {
                        parameter("include_changelog", false)
                    }
                    .body()
                ModrinthVersionsResult(versions = versions)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ModrinthVersionsResult(failed = true)
            }
        }

    /**
     * Streams [url] into [target]. [onProgress] receives 0f..1f when the
     * server provides a Content-Length, and 1f on completion. Returns false
     * for a non-2xx response or any transport/file error; the caller owns
     * cleanup of a partial file.
     */
    suspend fun downloadTo(
        url: String,
        target: File,
        onProgress: (Float) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = network.http.get(url)
            if (!response.status.isSuccess()) return@withContext false
            val total = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
            val channel = response.bodyAsChannel()
            val buffer = ByteArray(DOWNLOAD_BUFFER)
            var written = 0L
            target.outputStream().use { output ->
                while (true) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read < 0) break
                    if (read == 0) continue
                    output.write(buffer, 0, read)
                    written += read
                    if (total > 0) {
                        onProgress((written.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
            onProgress(1f)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }

    /** Canonical project page URL for a hit, used by the "open" action. */
    fun projectUrl(hit: ModrinthHit): String =
        "https://modrinth.com/mod/" + hit.slug.ifEmpty { hit.project_id }

    private companion object {
        const val SEARCH_URL = "https://api.modrinth.com/v2/search"
        const val PROJECT_URL = "https://api.modrinth.com/v2/project"
        const val DOWNLOAD_BUFFER = 64 * 1024
    }
}
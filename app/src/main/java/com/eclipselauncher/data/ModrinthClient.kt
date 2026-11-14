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
) : MetaResult<List<ModrinthHit>> {
    override val value: List<ModrinthHit> = this.hits
    override val isSuccess: Boolean = true
}

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
            // Return error result instead of throwing
            ModrinthSearchResult(
                hits = emptyList(),
                totalHits = 0,
                offset = 0,
                limit = limit,
                totalPages = 0,
                pageNumber = 1,
                isLastPage = true,
            )
        }
    }

    /** Canonical project page URL for a hit, used by the "open" action. */
    fun projectUrl(hit: ModrinthHit): String =
        "https://modrinth.com/mod/" + hit.slug.ifEmpty { hit.project_id }

    private companion object {
        const val SEARCH_URL = "https://api.modrinth.com/v2/search"
    }
}
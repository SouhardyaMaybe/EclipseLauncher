/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Fake implementation of ModrinthApi for testing.
 * Returns controlled test data for unit tests.
 */
package me.shadow.eclipselauncher.data

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fake ModrinthApi that returns controlled test data.
 * Use [setSearchResults] to control search results.
 * Use [setProjectVersions] to control version results.
 */
class FakeModrinthClient : ModrinthApi {

    // Test data storage
    private var searchResults: ModrinthSearchResult = ModrinthSearchResult(emptyList(), 0, 0, 25, 1, 1, true, false)
    private var projectVersions: ModrinthVersionsResult = ModrinthVersionsResult(emptyList(), false)
    private var projectData: ModrinthProject? = null

    // Configuration
    var shouldFailSearch = false
    var shouldFailVersions = false
    var shouldFailProject = false

    // Setters for test data
    fun setSearchResults(result: ModrinthSearchResult) {
        this.searchResults = result
    }

    fun setProjectVersions(result: ModrinthVersionsResult) {
        this.projectVersions = result
    }

    fun setProjectData(project: ModrinthProject?) {
        this.projectData = project
    }

    override suspend fun searchMods(
        query: String,
        limit: Int,
        offset: Int,
        projectType: String,
        loader: String?,
        gameVersion: String?,
    ): ModrinthSearchResult = withContext(Dispatchers.IO) {
        if (shouldFailSearch) {
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
        } else {
            // Return a page of the pre-configured results
            val hits = searchResults.hits.drop(offset).take(limit)
            val totalPages = (searchResults.totalHits + limit - 1) / limit
            val pageNumber = offset / limit + 1
            val isLastPage = (offset + limit) >= searchResults.totalHits
            ModrinthSearchResult(
                hits = hits,
                totalHits = searchResults.totalHits,
                offset = offset,
                limit = limit,
                totalPages = totalPages,
                pageNumber = pageNumber,
                isLastPage = isLastPage,
                failed = false,
            )
        }
    }

    override suspend fun projectVersions(projectId: String): ModrinthVersionsResult = withContext(Dispatchers.IO) {
        if (shouldFailVersions) {
            ModrinthVersionsResult(failed = true)
        } else {
            projectVersions
        }
    }

    override suspend fun projectById(projectId: String): ModrinthProject? = withContext(Dispatchers.IO) {
        if (shouldFailProject) null else projectData
    }

    override suspend fun versionById(versionId: String): ModrinthVersion? = withContext(Dispatchers.IO) {
        null // Not used in tests
    }

    override suspend fun downloadTo(
        url: String,
        target: File,
        onProgress: (Float) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        false // Not used in tests
    }

    override fun projectUrl(hit: ModrinthHit): String =
        "https://modrinth.com/mod/" + hit.slug.ifEmpty { hit.project_id }
}
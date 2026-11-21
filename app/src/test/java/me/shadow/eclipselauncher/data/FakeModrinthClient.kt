/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Fake implementation of ModrinthClient for testing.
 * Returns controlled test data for unit tests.
 */
package me.shadow.eclipselauncher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fake ModrinthClient that returns controlled test data.
 * Use [setSearchResults] to control search results.
 * Use [setProjectVersions] to control version results.
 */
class FakeModrinthClient : ModrinthClient(null) {

    // Test data storage
    private var searchResults: ModrinthSearchResult = ModrinthSearchResult(emptyList(), 0, 0, 25, 1, true, false)
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
        limit: Int = 25,
        offset: Int = 0,
        projectType: String = "mod",
        loader: String? = null,
        gameVersion: String? = null,
    ): ModrinthSearchResult = withContext(Dispatchers.IO) {
        if (shouldFailSearch) {
            return ModrinthSearchResult(
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

    override suspend fun projectVersions(projectId: String): ModrinthVersionsResult = withContext(Dispatchers.IO) {
        if (shouldFailVersions) {
            return ModrinthVersionsResult(failed = true)
        }
        return projectVersions
    }

    override suspend fun projectById(projectId: String): ModrinthProject? = withContext(Dispatchers.IO) {
        if (shouldFailProject) return null
        return projectData
    }
}
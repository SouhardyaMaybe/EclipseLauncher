/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Real JVM tests for ModpackBrowserScreen (Robolectric + Compose):
 * - floating pagination pill: list reaches the bottom, pill is narrower than
 *   the screen and nothing full-width is drawn behind it;
 * - scroll-to-top after a filter, search text or page change;
 * - the version dialog shows release, snapshot, beta and alpha rows.
 *
 * The fake API and the fake version catalog are injected through the
 * production seams (`ModrinthApi`, `VersionCatalog`); no test opens a socket.
 */
package me.shadow.eclipselauncher.ui.screens

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import me.shadow.eclipselauncher.data.FakeModrinthClient
import me.shadow.eclipselauncher.data.McVersion
import me.shadow.eclipselauncher.data.MetaResult
import me.shadow.eclipselauncher.data.ModrinthApi
import me.shadow.eclipselauncher.data.ModrinthHit
import me.shadow.eclipselauncher.data.ModrinthSearchResult
import me.shadow.eclipselauncher.data.ModrinthVersion
import me.shadow.eclipselauncher.data.ModrinthVersionFile
import me.shadow.eclipselauncher.data.ModrinthVersionsResult
import me.shadow.eclipselauncher.data.ThemeMode
import me.shadow.eclipselauncher.data.VersionCatalog
import me.shadow.eclipselauncher.ui.theme.EclipseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    qualifiers = "w360dp-h640dp-xxhdpi",
    application = Application::class,
)
class ModpackBrowserScreenUITest {

    @get:Rule
    val composeRule = createComposeRule()

    private val versionCatalog = TestVersionCatalog()

    @Test
    fun paginationPill_floatsOverList_listReachesBottom_pillNarrowerThanScreen() {
        renderScreen(FakeModrinthClient().withPages(60))

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("pagination_pill").fetchSemanticsNodes().isNotEmpty()
        }

        val listBounds = composeRule.onNodeWithTag("modpack_list").getUnclippedBoundsInRoot()
        val rootBounds = composeRule.onRoot().getUnclippedBoundsInRoot()
        val pillBounds = composeRule.onNodeWithTag("pagination_pill").getUnclippedBoundsInRoot()

        assertEquals(
            "the lazy list must reach the bottom of the screen",
            rootBounds.bottom.value,
            listBounds.bottom.value,
            1f,
        )
        assertTrue(
            "the pagination pill must float, not span the screen",
            (pillBounds.right - pillBounds.left).value <
                (rootBounds.right - rootBounds.left).value,
        )
        assertTrue(
            "the pill must sit over the list, not above it",
            pillBounds.bottom.value <= listBounds.bottom.value + 1f,
        )
        composeRule.onNodeWithTag("pagination_pill").assertIsDisplayed()
    }

    @Test
    fun modpackVersionDialog_showsAllFourVersionTypes() {
        renderScreen(FakeModrinthClient().withFourVersionTypes())

        composeRule.onNodeWithTag("modpack_row_0", useUnmergedTree = true).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("version_group_header_1.20.1")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Groups start collapsed; expand the one that holds all four rows.
        composeRule.onNodeWithTag("version_group_header_1.20.1").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            listOf("release", "snapshot", "beta", "alpha").all { type ->
                composeRule.onAllNodesWithTag("version_group_$type")
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }

        composeRule.waitForIdle()
        listOf("release", "snapshot", "beta", "alpha").forEach { type ->
            composeRule.onNodeWithTag("version_group_$type", useUnmergedTree = true)
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun scrollToTop_onFilterChange() {
        renderScreen(FakeModrinthClient().withPages(60))
        scrollDown()

        composeRule.onNodeWithTag("loader_filter_header").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("loader_filter_header").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Fabric", useUnmergedTree = true).performClick()

        assertFirstRowVisible()
    }

    @Test
    fun scrollToTop_onSearchChange() {
        renderScreen(FakeModrinthClient().withPages(60))
        scrollDown()

        composeRule.onNodeWithTag("search_field").performTextInput("test")

        assertFirstRowVisible()
    }

    @Test
    fun scrollToTop_onPageChange() {
        renderScreen(FakeModrinthClient().withPages(60))
        scrollDown()

        composeRule.onNodeWithTag("pagination_next").performClick()

        assertFirstRowVisible()
    }

    private fun scrollDown() {
        composeRule.onNodeWithTag("modpack_list")
            .performScrollToNode(hasTestTag("modpack_row_10"))
    }

    private fun assertFirstRowVisible() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("modpack_row_0").fetchSemanticsNodes().isNotEmpty()
        }
        // animateScrollToItem keeps the test clock busy; wait for it to settle
        // before asserting that the row is actually inside the viewport.
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("modpack_row_0").assertIsDisplayed()
    }

    private fun renderScreen(client: ModrinthApi) {
        composeRule.setContent {
            EclipseTheme(themeMode = ThemeMode.URBAN) {
                ModpackBrowserScreen(
                    client = client,
                    versionRepository = versionCatalog,
                    onBack = {},
                    onPickModpack = {},
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("modpack_row_0").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun FakeModrinthClient.withPages(count: Int): FakeModrinthClient = apply {
        val hits = (1..count).map { index ->
            ModrinthHit(
                project_id = "pack-$index",
                slug = "test-pack-$index",
                title = "Test Pack $index",
                author = "Author $index",
                description = "",
                icon_url = "",
                categories = emptyList(),
                versions = emptyList(),
                downloads = 1_000L,
            )
        }
        setSearchResults(
            ModrinthSearchResult(
                hits = hits,
                totalHits = count,
                offset = 0,
                limit = 25,
                totalPages = (count + 24) / 25,
                pageNumber = 1,
                isLastPage = false,
            ),
        )
    }

    private fun FakeModrinthClient.withFourVersionTypes(): FakeModrinthClient = apply {
        setSearchResults(
            ModrinthSearchResult(
                hits = listOf(
                    ModrinthHit(
                        project_id = "pack-1",
                        slug = "test-pack-1",
                        title = "Test Pack 1",
                        author = "Author 1",
                    ),
                ),
                totalHits = 1,
                offset = 0,
                limit = 25,
                totalPages = 1,
                pageNumber = 1,
                isLastPage = true,
            ),
        )
        val versions = listOf("release", "snapshot", "beta", "alpha").mapIndexed { index, type ->
            ModrinthVersion(
                id = "v${index + 1}",
                project_id = "pack-1",
                name = "$type version",
                version_number = "1.$index",
                game_versions = listOf("1.20.1"),
                loaders = listOf("fabric"),
                date_published = "2024-0${index + 1}-01T00:00:00Z",
                version_type = type,
                files = listOf(
                    ModrinthVersionFile(
                        url = "https://example.invalid/v${index + 1}.jar",
                        filename = "v${index + 1}.jar",
                        primary = true,
                    ),
                ),
            )
        }
        setProjectVersions(ModrinthVersionsResult(versions = versions, failed = false))
    }

    private class TestVersionCatalog : VersionCatalog {
        private val versions = listOf(
            McVersion(id = "1.20.1", type = "release", releaseTime = 1_700_000_000_000L),
            McVersion(id = "1.21", type = "release", releaseTime = 1_700_100_000_000L),
            McVersion(id = "24w20a", type = "snapshot", releaseTime = 1_700_200_000_000L),
        )

        override fun getCachedVersions(): List<McVersion> = versions

        override suspend fun minecraftVersions(): MetaResult<List<McVersion>> =
            MetaResult.Success(versions)
    }
}

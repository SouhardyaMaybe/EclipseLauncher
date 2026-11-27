/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Real JVM tests for the skin dialog's full-screen control. These render the
 * production `SkinCustomizationDialog` for both account kinds (the same
 * `canCustomize` wiring AccountsScreen uses), not a button in isolation.
 *
 * The first run of these tests is expected to fail on the pre-fix code:
 *  - the icon has a 24dp layout box but 12dp inner padding, so it draws 0x0;
 *  - the old onClick only toggled private state; the dialog ignored the
 *    callback and stayed a fixed 240dp stage, so the canvas never grew.
 */
package me.shadow.eclipselauncher.ui.components

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.data.Account
import me.shadow.eclipselauncher.data.AccountKind
import me.shadow.eclipselauncher.data.MediaStorage
import me.shadow.eclipselauncher.data.ThemeMode
import me.shadow.eclipselauncher.ui.character.FULLSCREEN_BUTTON_TAG
import me.shadow.eclipselauncher.ui.character.SKIN_MODEL_CANVAS_TAG
import me.shadow.eclipselauncher.ui.theme.EclipseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    qualifiers = "w360dp-h640dp-xxhdpi",
    // The real app initializes MMKV/Hilt in onCreate; these tests need a plain
    // Application so Robolectric can run them on the JVM.
    application = Application::class,
)
class SkinCustomizationDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = RuntimeEnvironment.getApplication()
    private val media = MediaStorage(context)
    private val enterDescription = context.getString(R.string.cd_enter_fullscreen)
    private val exitDescription = context.getString(R.string.cd_exit_fullscreen)
    private val displayMetrics = context.resources.displayMetrics
    private val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
    private val screenHeightDp = displayMetrics.heightPixels / displayMetrics.density

    @Test
    fun microsoftDialog_iconHasNonZeroBounds() = assertIconBounds(AccountKind.MICROSOFT)

    @Test
    fun localDialog_iconHasNonZeroBounds() = assertIconBounds(AccountKind.LOCAL)

    @Test
    fun microsoftDialog_enterFullscreenFillsScreenAndExitRestores() =
        assertFullscreenToggle(AccountKind.MICROSOFT)

    @Test
    fun localDialog_enterFullscreenFillsScreenAndExitRestores() =
        assertFullscreenToggle(AccountKind.LOCAL)

    @Test
    fun fullscreenStateSurvivesSaveRestore() {
        val restorer = StateRestorationTester(composeRule)
        restorer.setContent {
            EclipseTheme(themeMode = ThemeMode.URBAN) {
                SkinCustomizationDialog(
                    account = localAccount("local-restore"),
                    media = media,
                    onUpdate = {},
                    onDismiss = {},
                    canCustomize = true,
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(FULLSCREEN_BUTTON_TAG).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(exitDescription, useUnmergedTree = true)
            .assertExists()

        restorer.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(exitDescription, useUnmergedTree = true)
            .assertExists()
        val canvas = composeRule.onNodeWithTag(SKIN_MODEL_CANVAS_TAG).getUnclippedBoundsInRoot()
        assertEquals("restored full-screen width", screenWidthDp.toDouble(), canvas.width.value.toDouble(), 1.5)
        assertEquals("restored full-screen height", screenHeightDp.toDouble(), canvas.height.value.toDouble(), 1.5)
    }

    @Test
    fun fullscreenDrawables_haveNonZeroIntrinsicSizeAndRealPathData() {
        val enterPaths = pathData(R.drawable.ic_fullscreen_24)
        val exitPaths = pathData(R.drawable.ic_fullscreen_exit_24)

        assertTrue("fullscreen drawable has no <path> data", enterPaths.isNotEmpty())
        assertTrue("fullscreen exit drawable has no <path> data", exitPaths.isNotEmpty())
        assertTrue(
            "fullscreen path data is blank",
            enterPaths.all { it.isNotBlank() && it.contains("M") },
        )
        assertTrue(
            "fullscreen exit path data is blank",
            exitPaths.all { it.isNotBlank() && it.contains("M") },
        )

        var enterIntrinsic: Size? = null
        var exitIntrinsic: Size? = null
        composeRule.setContent {
            val enterPainter = painterResource(R.drawable.ic_fullscreen_24)
            val exitPainter = painterResource(R.drawable.ic_fullscreen_exit_24)
            SideEffect {
                enterIntrinsic = enterPainter.intrinsicSize
                exitIntrinsic = exitPainter.intrinsicSize
            }
        }
        composeRule.waitForIdle()

        val enterSize = requireNotNull(enterIntrinsic) { "fullscreen painter was not composed" }
        val exitSize = requireNotNull(exitIntrinsic) { "fullscreen exit painter was not composed" }
        assertTrue("fullscreen painter width is zero", enterSize.width > 0f)
        assertTrue("fullscreen painter height is zero", enterSize.height > 0f)
        assertTrue("fullscreen exit painter width is zero", exitSize.width > 0f)
        assertTrue("fullscreen exit painter height is zero", exitSize.height > 0f)
    }

    @Test
    fun fullscreenIconTint_hasAtLeast3To1ContrastWithCanvas() {
        var contrast = 0.0
        composeRule.setContent {
            EclipseTheme(themeMode = ThemeMode.URBAN) {
                val scheme = MaterialTheme.colorScheme
                SideEffect {
                    val canvas =
                        compositeOver(scheme.surfaceVariant.copy(alpha = 0.45f), scheme.surface)
                    contrast = contrastRatio(scheme.onSurface, canvas)
                }
            }
        }
        composeRule.waitForIdle()
        assertTrue("icon tint contrast was $contrast, expected >= 3.0", contrast >= 3.0)
    }

    private fun assertIconBounds(kind: AccountKind) {
        showDialog(kind)
        val icon = composeRule.onNodeWithContentDescription(
            enterDescription,
            useUnmergedTree = true,
        )
        icon.assertExists()
        val bounds = icon.getUnclippedBoundsInRoot()
        assertTrue("icon width was ${bounds.width}", bounds.width > 0.dp)
        assertTrue("icon height was ${bounds.height}", bounds.height > 0.dp)
    }

    private fun assertFullscreenToggle(kind: AccountKind) {
        showDialog(kind)

        val canvasBefore = composeRule.onNodeWithTag(SKIN_MODEL_CANVAS_TAG)
            .getUnclippedBoundsInRoot()
        assertTrue("normal canvas width was ${canvasBefore.width}", canvasBefore.width > 0.dp)
        assertTrue("normal canvas height was ${canvasBefore.height}", canvasBefore.height > 0.dp)

        val buttonBefore = composeRule.onNodeWithTag(FULLSCREEN_BUTTON_TAG)
        buttonBefore.assertExists()
        buttonBefore.assertWidthIsAtLeast(48.dp)
        buttonBefore.assertHeightIsAtLeast(48.dp)

        buttonBefore.performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(exitDescription, useUnmergedTree = true)
            .assertExists()
        val canvasFull = composeRule.onNodeWithTag(SKIN_MODEL_CANVAS_TAG)
            .getUnclippedBoundsInRoot()
        assertEquals("full-screen width", screenWidthDp.toDouble(), canvasFull.width.value.toDouble(), 1.5)
        assertEquals("full-screen height", screenHeightDp.toDouble(), canvasFull.height.value.toDouble(), 1.5)

        val buttonFull = composeRule.onNodeWithTag(FULLSCREEN_BUTTON_TAG)
        buttonFull.assertExists()
        buttonFull.assertWidthIsAtLeast(48.dp)
        buttonFull.assertHeightIsAtLeast(48.dp)

        composeRule.onNodeWithTag(FULLSCREEN_BUTTON_TAG).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(enterDescription, useUnmergedTree = true)
            .assertExists()
        val canvasAfter = composeRule.onNodeWithTag(SKIN_MODEL_CANVAS_TAG)
            .getUnclippedBoundsInRoot()
        assertEquals(
            "canvas width was not restored",
            canvasBefore.width.value.toDouble(),
            canvasAfter.width.value.toDouble(),
            1.0,
        )
        assertEquals(
            "canvas height was not restored",
            canvasBefore.height.value.toDouble(),
            canvasAfter.height.value.toDouble(),
            1.0,
        )
    }

    private fun showDialog(kind: AccountKind) {
        val account =
            if (kind == AccountKind.LOCAL) {
                localAccount("local-fullscreen")
            } else {
                Account(
                    id = "microsoft-fullscreen",
                    kind = AccountKind.MICROSOFT,
                    username = "MicrosoftTest",
                )
            }
        composeRule.setContent {
            EclipseTheme(themeMode = ThemeMode.URBAN) {
                SkinCustomizationDialog(
                    account = account,
                    media = media,
                    onUpdate = {},
                    onDismiss = {},
                    canCustomize = kind == AccountKind.LOCAL,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun localAccount(id: String) = Account(
        id = id,
        kind = AccountKind.LOCAL,
        username = "LocalTest",
    )

    /** Reads `android:pathData` from the compiled vector drawable resource. */
    private fun pathData(resId: Int): List<String> {
        val parser = context.resources.getXml(resId)
        val androidNamespace = "http://schemas.android.com/apk/res/android"
        val paths = mutableListOf<String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "path") {
                val data = parser.getAttributeValue(androidNamespace, "pathData")
                    ?: parser.getAttributeValue(null, "pathData")
                if (!data.isNullOrBlank()) paths += data
            }
            event = parser.next()
        }
        return paths
    }

    private fun compositeOver(foreground: Color, background: Color): Color {
        val alpha = foreground.alpha
        return Color(
            red = foreground.red * alpha + background.red * (1f - alpha),
            green = foreground.green * alpha + background.green * (1f - alpha),
            blue = foreground.blue * alpha + background.blue * (1f - alpha),
            alpha = 1f,
        )
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val first = a.luminance().toDouble() + 0.05
        val second = b.luminance().toDouble() + 0.05
        return if (first > second) first / second else second / first
    }
}

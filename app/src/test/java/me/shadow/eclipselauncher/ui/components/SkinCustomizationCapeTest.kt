/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Real JVM wiring tests for capes in the production dialog: a local Pick cape
 * must reach the model, Clear cape must remove it, and a Microsoft account
 * with a stored cape must show it. The debug diagnostic overlay is the
 * observable seam.
 */
package me.shadow.eclipselauncher.ui.components

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.data.Account
import me.shadow.eclipselauncher.data.AccountKind
import me.shadow.eclipselauncher.data.MediaStorage
import me.shadow.eclipselauncher.data.ThemeMode
import me.shadow.eclipselauncher.ui.character.CAPE_DIAGNOSTICS_TAG
import me.shadow.eclipselauncher.ui.theme.EclipseTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    qualifiers = "w360dp-h640dp-xxhdpi",
    application = Application::class,
)
class SkinCustomizationCapeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = RuntimeEnvironment.getApplication()
    private val media = MediaStorage(context)
    private val registry = FakeRegistry()
    private val pickCapeLabel = context.getString(R.string.action_pick_cape)
    private val clearCapeLabel = context.getString(R.string.action_clear_cape)

    @Volatile
    private var latest: Account? = null

    @Test
    fun localPickCapeReachesTheModelThenClearRemovesIt() {
        val uri = contentUri(capePng())
        show(localAccount("local-cape"), canCustomize = true)

        val initial = capeText()
        assertTrue(initial, initial.contains("cape faces: 0"))
        assertTrue(initial, initial.contains("reached view: false"))

        composeRule.onNodeWithText(pickCapeLabel).performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle { registry.deliver(uri) }
        composeRule.waitUntil(5_000) { latest?.capeFileName != null }
        waitForDiagnostic("cape faces: 6")

        val picked = capeText()
        assertTrue(picked, picked.contains("reached view: true"))
        assertTrue(picked, picked.contains("decode: ok"))
        assertTrue(picked, picked.contains("uv outer=1,1,10,16"))
        assertTrue(picked, picked.contains("file: ${latest?.capeFileName}"))

        composeRule.onNodeWithText(clearCapeLabel).performClick()
        composeRule.waitUntil(5_000) { latest?.capeFileName == null }
        waitForDiagnostic("cape faces: 0")
        assertTrue(capeText().contains("reached view: false"))
    }

    @Test
    fun microsoftAccountWithStoredCapeReachesTheModel() {
        val name = "microsoft-cape.png"
        media.file(name).writeBytes(capePng())
        show(
            Account(
                id = "microsoft-cape",
                kind = AccountKind.MICROSOFT,
                username = "CapeUser",
                capeFileName = name,
            ),
            canCustomize = false,
        )

        waitForDiagnostic("cape faces: 6")
        val text = capeText()
        assertTrue(text, text.contains("reached view: true"))
        assertTrue(text, text.contains("decode: ok"))
        assertTrue(text, text.contains("uv outer=1,1,10,16"))
        assertTrue(text, text.contains("uv inner=12,1,10,16"))
    }

    private fun show(initial: Account, canCustomize: Boolean) {
        composeRule.setContent {
            EclipseTheme(themeMode = ThemeMode.URBAN) {
                CompositionLocalProvider(
                    LocalActivityResultRegistryOwner provides TestOwner(registry),
                ) {
                    var account by remember { mutableStateOf(initial) }
                    SideEffect { latest = account }
                    SkinCustomizationDialog(
                        account = account,
                        media = media,
                        onUpdate = { updated ->
                            account = updated
                            latest = updated
                        },
                        onDismiss = {},
                        canCustomize = canCustomize,
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun waitForDiagnostic(fragment: String) {
        composeRule.waitUntil(5_000) {
            runCatching { capeText().contains(fragment) }.getOrDefault(false)
        }
        assertTrue(capeText(), capeText().contains(fragment))
    }

    private fun capeText(): String =
        composeRule.onNodeWithTag(CAPE_DIAGNOSTICS_TAG, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .joinToString("\n") { it.text }

    private fun localAccount(id: String) = Account(
        id = id,
        kind = AccountKind.LOCAL,
        username = "CapeUser",
    )

    private fun capePng(): ByteArray {
        val bitmap = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(210, 120, 30))
        val out = ByteArrayOutputStream()
        assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
        bitmap.recycle()
        return out.toByteArray()
    }

    private fun contentUri(bytes: ByteArray): Uri {
        val uri = Uri.parse("content://me.shadow.eclipselauncher.test/picked-cape.png")
        shadowOf(context.contentResolver).registerInputStream(uri, ByteArrayInputStream(bytes))
        return uri
    }

    private class TestOwner(
        override val activityResultRegistry: ActivityResultRegistry,
    ) : ActivityResultRegistryOwner

    private class FakeRegistry : ActivityResultRegistry() {
        private var pendingRequest = -1

        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            pendingRequest = requestCode
        }

        fun deliver(uri: Uri) {
            check(pendingRequest >= 0) { "No picker launch was registered" }
            dispatchResult(pendingRequest, Activity.RESULT_OK, Intent().setData(uri))
        }
    }
}

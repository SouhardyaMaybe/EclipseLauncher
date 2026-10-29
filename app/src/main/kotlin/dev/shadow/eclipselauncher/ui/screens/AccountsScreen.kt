/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.shadow.eclipselauncher.data.LauncherState
import dev.shadow.eclipselauncher.designsystem.theme.MojoPalette
import dev.shadow.eclipselauncher.designsystem.theme.MojoTheme
import dev.shadow.eclipselauncher.designsystem.theme.MojoType
import dev.shadow.eclipselauncher.ui.components.MojoDialog
import dev.shadow.eclipselauncher.ui.components.MojoMineButton
import dev.shadow.eclipselauncher.ui.components.MojoMineTextField

private enum class AccountsStage { METHODS, LOCAL, MICROSOFT, ELYBY }

private val LocalUsername = Regex("^[A-Za-z0-9_]{3,16}$")

/**
 * `SelectAuthFragment` and its three login fragments.
 *
 * The account list itself lives in the launcher account spinner, so this
 * screen starts directly on the method card. Android back pops the current
 * login fragment exactly like the reference back stack. Microsoft and Ely.by
 * use the reference OAuth URLs; this build has no token exchange, so a
 * returned code and an unknown OAuth error both end in `Error` +
 * `Unknown authentication error`. An explicit denial only pops the WebView.
 */
@Composable
fun AccountsScreen(
    state: LauncherState,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onAuthError: (message: String, returnHome: Boolean) -> Unit,
) {
    var stage by rememberSaveable { mutableStateOf(AccountsStage.METHODS.name) }
    var localName by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val current = runCatching { AccountsStage.valueOf(stage) }
        .getOrDefault(AccountsStage.METHODS)

    fun popLogin() {
        stage = AccountsStage.METHODS.name
    }

    BackHandler(enabled = true) {
        if (current == AccountsStage.METHODS) onBack() else popLogin()
    }

    MojoTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MojoPalette.Background),
        ) {
            when (current) {
                AccountsStage.METHODS -> AuthMethodCard(
                    onMicrosoft = { stage = AccountsStage.MICROSOFT.name },
                    onElyBy = { stage = AccountsStage.ELYBY.name },
                    onLocal = {
                        localName = ""
                        localError = false
                        stage = AccountsStage.LOCAL.name
                    },
                )

                AccountsStage.LOCAL -> LocalLoginCard(
                    name = localName,
                    onNameChange = {
                        localName = it
                        localError = false
                    },
                    onLogin = {
                        if (LocalUsername.matches(localName)) {
                            state.addLocalAccount(localName)
                            Toast.makeText(
                                context,
                                context.getString(dev.shadow.eclipselauncher.R.string.mojo_login_done),
                                Toast.LENGTH_SHORT,
                            ).show()
                            onDone()
                        } else {
                            localError = true
                        }
                    },
                )

                AccountsStage.MICROSOFT -> OAuthWebView(
                    trackedUrl = "ms-xal-00000000402b5328",
                    authUrl = MICROSOFT_AUTH_URL,
                    onExit = ::popLogin,
                    onAccessDenied = ::popLogin,
                    onUnknownError = { message ->
                        popLogin()
                        onAuthError(message, false)
                    },
                    onCode = {
                        Toast.makeText(
                            context,
                            context.getString(dev.shadow.eclipselauncher.R.string.mojo_authenticating),
                            Toast.LENGTH_SHORT,
                        ).show()
                        onAuthError(UNKNOWN_AUTH_ERROR, true)
                    },
                )

                AccountsStage.ELYBY -> OAuthWebView(
                    trackedUrl = "internalredirect",
                    authUrl = ELYBY_AUTH_URL,
                    onExit = ::popLogin,
                    onAccessDenied = ::popLogin,
                    onUnknownError = { message ->
                        popLogin()
                        onAuthError(message, false)
                    },
                    onCode = {
                        Toast.makeText(
                            context,
                            context.getString(dev.shadow.eclipselauncher.R.string.mojo_authenticating),
                            Toast.LENGTH_SHORT,
                        ).show()
                        onAuthError(UNKNOWN_AUTH_ERROR, true)
                    },
                )
            }
        }
    }

    if (localError && current == AccountsStage.LOCAL) {
        MojoDialog(
            onDismiss = { localError = false },
            confirmLabel = "OK",
            onConfirm = { localError = false },
            title = "Unsuitable username",
            message = "The username must be between 3–16 characters long, and must only contain latin letters, numbers 0–9 and underscores.",
        )
    }
}

@Composable
private fun AuthMethodCard(
    onMicrosoft: () -> Unit,
    onElyBy: () -> Unit,
    onLocal: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MojoPalette.StatusBar)
                .padding(horizontal = 25.dp, vertical = 32.dp),
        ) {
            MojoMineButton(
                text = "Microsoft account",
                onClick = onMicrosoft,
            )
            Spacer(Modifier.height(20.dp))
            MojoMineButton(
                text = "Ely.by account",
                onClick = onElyBy,
            )
            Spacer(Modifier.height(20.dp))
            MojoMineButton(
                text = "Local account",
                onClick = onLocal,
            )
        }
    }
}

@Composable
private fun LocalLoginCard(
    name: String,
    onNameChange: (String) -> Unit,
    onLogin: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MojoPalette.StatusBar)
                .padding(start = 25.dp, top = 42.dp, end = 25.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = "Username",
                style = MojoType.bodySmall,
                color = MojoPalette.PrimaryText,
            )
            Spacer(Modifier.height(2.dp))
            MojoMineTextField(
                value = name,
                onValueChange = onNameChange,
            )
            Spacer(Modifier.height(12.dp))
            MojoMineButton(
                text = "Login",
                onClick = onLogin,
                height = 42.dp,
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun OAuthWebView(
    trackedUrl: String,
    authUrl: String,
    onExit: () -> Unit,
    onAccessDenied: () -> Unit,
    onUnknownError: (String) -> Unit,
    onCode: (String) -> Unit,
) {
    val context = LocalContext.current
    var completed by remember { mutableStateOf(false) }
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }

    BackHandler(enabled = true) {
        if (webView.canGoBack()) webView.goBack() else onExit()
    }

    AndroidView(
        factory = { webView },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            view.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    url: String,
                ): Boolean {
                    if (completed) return true
                    if (url.startsWith(trackedUrl)) {
                        completed = true
                        handleCompletion(
                            url = url,
                            onAccessDenied = onAccessDenied,
                            onUnknownError = onUnknownError,
                            onCode = onCode,
                        )
                        return true
                    }
                    return false
                }

                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    if (completed) return
                    if (url.startsWith(trackedUrl)) {
                        completed = true
                        handleCompletion(
                            url = url,
                            onAccessDenied = onAccessDenied,
                            onUnknownError = onUnknownError,
                            onCode = onCode,
                        )
                    }
                }
            }
        },
        onRelease = { view ->
            view.stopLoading()
            view.destroy()
        },
    )

    LaunchedEffect(Unit) {
        CookieManager.getInstance().removeAllCookies {
            webView.clearHistory()
            webView.clearCache(true)
            webView.clearFormData()
            webView.loadUrl(authUrl)
        }
    }
}

private fun handleCompletion(
    url: String,
    onAccessDenied: () -> Unit,
    onUnknownError: (String) -> Unit,
    onCode: (String) -> Unit,
) {
    val uri = Uri.parse(url)
    val error = uri.getQueryParameter("error")
    val code = uri.getQueryParameter("code")
    if (code == null) {
        if (error == "access_denied") {
            onAccessDenied()
        } else {
            val message = uri.getQueryParameter("error_description")
                ?: error
                ?: UNKNOWN_AUTH_ERROR
            onUnknownError(message)
        }
        return
    }
    onCode(code)
}

private const val UNKNOWN_AUTH_ERROR = "Unknown authentication error"

private const val MICROSOFT_AUTH_URL =
    "https://login.live.com/oauth20_authorize.srf" +
        "?client_id=00000000402b5328" +
        "&response_type=code" +
        "&scope=service%3A%3Auser.auth.xboxlive.com%3A%3AMBI_SSL" +
        "&redirect_url=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf"

private const val ELYBY_AUTH_URL =
    "https://account.ely.by/oauth2/v1" +
        "?client_id=mojolauncher2" +
        "&redirect_uri=internalredirect%3A%2F%2Fcomplete" +
        "&response_type=code" +
        "&scope=account_info%20offline_access%20minecraft_server_session"

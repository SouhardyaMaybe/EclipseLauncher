/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * MICROSOFT SIGN-IN: a WebView over the login.live.com authorize page; the
 * redirect to oauth20_desktop.srf is intercepted, the authorization code is
 * exchanged through the full Xbox Live -> Minecraft services chain, and the
 * resulting account is handed back. While the chain runs, a progress panel
 * replaces the WebView.
 */
package com.eclipselauncher.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import com.eclipselauncher.R
import com.eclipselauncher.data.Account
import com.eclipselauncher.data.AccountKind
import com.eclipselauncher.data.MicrosoftAuth
import com.eclipselauncher.data.MsAuthError
import com.eclipselauncher.data.MsAuthException
import com.eclipselauncher.data.MsAuthStage
import com.eclipselauncher.data.RedirectResult
import com.eclipselauncher.data.newId

private sealed interface AuthUi {
    data object Browser : AuthUi
    data class Progress(val stage: MsAuthStage) : AuthUi
    data class Failed(val error: MsAuthError, val message: String?) : AuthUi
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MicrosoftAuthScreen(
    auth: MicrosoftAuth,
    onSignedIn: (Account) -> Unit,
    onCancel: () -> Unit,
) {
    var ui by remember { mutableStateOf<AuthUi>(AuthUi.Browser) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ms_auth_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = ui) {
                AuthUi.Browser -> AuthWebView(
                    auth = auth,
                    onExit = onCancel,
                    onCode = { code ->
                        ui = AuthUi.Progress(MsAuthStage.LIVE)
                        scope.launch {
                            try {
                                val result = auth.login(code) { stage ->
                                    ui = AuthUi.Progress(stage)
                                }
                                onSignedIn(
                                    Account(
                                        id = newId(),
                                        kind = AccountKind.MICROSOFT,
                                        username = result.username,
                                        uuid = result.uuid,
                                        xuid = result.xuid,
                                        refreshToken = result.refreshToken,
                                        expiresAtEpochSeconds = result.expiresAtEpochSeconds,
                                    ),
                                )
                            } catch (e: MsAuthException) {
                                ui = AuthUi.Failed(e.error, e.message)
                            } catch (e: Exception) {
                                ui = AuthUi.Failed(MsAuthError.UNKNOWN, e.message)
                            }
                        }
                    },
                )

                is AuthUi.Progress -> Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(
                            when (state.stage) {
                                MsAuthStage.LIVE -> R.string.ms_stage_live
                                MsAuthStage.XBOX -> R.string.ms_stage_xbl
                                MsAuthStage.XSTS -> R.string.ms_stage_xsts
                                MsAuthStage.PROFILE -> R.string.ms_stage_profile
                            },
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                }

                is AuthUi.Failed -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.ms_error_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(authErrorMessage(state.error)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onCancel) {
                        Text(stringResource(R.string.action_try_again))
                    }
                }
            }
        }
    }
}

private fun authErrorMessage(error: MsAuthError): Int = when (error) {
    MsAuthError.DENIED -> R.string.ms_error_denied
    MsAuthError.NETWORK -> R.string.ms_error_network
    MsAuthError.NO_XBOX_PROFILE -> R.string.ms_error_no_xbox
    MsAuthError.REGION -> R.string.ms_error_region
    MsAuthError.ADULT_VERIFY -> R.string.ms_error_adult
    MsAuthError.CHILD -> R.string.ms_error_child
    MsAuthError.NO_MINECRAFT -> R.string.ms_error_no_mc
    MsAuthError.UNKNOWN -> R.string.ms_error_unknown
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AuthWebView(
    auth: MicrosoftAuth,
    onExit: () -> Unit,
    onCode: (String) -> Unit,
) {
    val context = LocalContext.current
    var handled by remember { mutableStateOf(false) }
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }

    // The WebView's own back history wins over screen navigation.
    BackHandler(enabled = webView.canGoBack()) {
        webView.goBack()
    }

    AndroidView(
        factory = { webView },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            view.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    val url = request.url.toString()
                    when (val result = auth.parseRedirect(url)) {
                        is RedirectResult.Success -> {
                            if (!handled) {
                                handled = true
                                onCode(result.code)
                            }
                            return true
                        }
                        is RedirectResult.Denied -> {
                            if (!handled) {
                                handled = true
                                onExit()
                            }
                            return true
                        }
                        is RedirectResult.Failure -> {
                            if (!handled) {
                                handled = true
                                onExit()
                            }
                            return true
                        }
                        null -> return false
                    }
                }
            }
            if (view.url == null) {
                view.loadUrl(auth.authorizeUrl())
            }
        },
    )
}

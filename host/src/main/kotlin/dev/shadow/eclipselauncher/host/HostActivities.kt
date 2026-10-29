/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.host

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * The reference launcher's AWT/JVM window. It is visual-only in this build:
 * there is no JVM to attach to the surface, so the activity is not launched by
 * any launcher flow.
 */
class AwtActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#242424")
        window.navigationBarColor = Color.parseColor("#181818")
        hideSystemBars()
        setContent {
            AwtWindowScreen(onExit = { finish() })
        }
    }
}

/**
 * The reference launcher's in-game control HUD. It is visual-only in this
 * build because the game runtime does not exist yet.
 */
class GameHudActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#242424")
        window.navigationBarColor = Color.parseColor("#181818")
        hideSystemBars()
        setContent {
            GameHudScreen(onExit = { finish() })
        }
    }
}

class MissingStorageActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#242424")
        window.navigationBarColor = Color.parseColor("#181818")
        setContent {
            MissingStorageScreen()
        }
    }
}

class ExitActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#242424")
        window.navigationBarColor = Color.parseColor("#181818")
        val code = intent.getIntExtra("code", -1)
        val isSignal = intent.getBooleanExtra("isSignal", false)
        setContent {
            ExitScreen(
                code = code,
                isSignal = isSignal,
                onFinished = { finish() },
            )
        }
    }
}

class FatalErrorActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#242424")
        window.navigationBarColor = Color.parseColor("#181818")
        val extras = intent.extras
        if (extras == null) {
            finish()
            return
        }
        val storageAllow = extras.getBoolean("storageAllow", false)
        val savePath = extras.getString("savePath")
        val throwable = readThrowable(extras)
        val stackTrace = throwable?.let { Log.getStackTraceString(it) } ?: "<null>"
        setContent {
            FatalErrorScreen(
                storageAllow = storageAllow,
                savePath = savePath,
                stackTrace = stackTrace,
                onFinished = { finish() },
                onRestart = { restartLauncher() },
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun readThrowable(extras: Bundle): Throwable? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getSerializable("throwable", Throwable::class.java)
        } else {
            extras.getSerializable("throwable") as? Throwable
        }

    private fun restartLauncher() {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launch)
        }
        finish()
    }
}

private fun ComponentActivity.hideSystemBars() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowInsetsControllerCompat(window, window.decorView).apply {
        hide(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

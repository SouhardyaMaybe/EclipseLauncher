/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.host

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager

class GameActivity : Activity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.parseColor("#242424")
        window.navigationBarColor = Color.parseColor("#181818")
        setContentView(
            processScreen(
                context = this,
                title = getString(R.string.host_game_title),
                body = getString(R.string.host_game_body),
            ),
        )
    }
}

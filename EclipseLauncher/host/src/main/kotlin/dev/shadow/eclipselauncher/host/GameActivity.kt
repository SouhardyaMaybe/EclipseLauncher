/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.host

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager

class GameActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(
            processScreen(
                context = this,
                title = getString(R.string.host_game_title),
                body = getString(R.string.host_game_body),
            ),
        )
    }
}

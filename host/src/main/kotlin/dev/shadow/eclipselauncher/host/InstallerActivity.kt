/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.host

import android.app.Activity
import android.os.Bundle

class InstallerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            processScreen(
                context = this,
                title = getString(R.string.host_installer_title),
                body = getString(R.string.host_installer_body),
            ),
        )
    }
}

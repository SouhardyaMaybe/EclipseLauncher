/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.host

import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns

/**
 * Receives the URI selected by the launcher and starts the installer process.
 * The activity is deliberately launched immediately: there is no second
 * confirmation screen between choosing a .jar and running it.
 */
class InstallerActivity : Activity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#242424")
        window.navigationBarColor = Color.parseColor("#181818")

        val uri = readUri()
        val name = uri?.let(::displayName)
        val body = if (name == null) {
            getString(R.string.host_installer_unknown)
        } else {
            getString(R.string.host_installer_selected, name)
        }

        setContentView(
            processScreen(
                context = this,
                title = getString(R.string.host_installer_title),
                body = body,
            ),
        )
    }

    @Suppress("DEPRECATION")
    private fun readUri(): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("modUri", Uri::class.java)
        } else {
            intent.getParcelableExtra("modUri")
        }

    private fun displayName(uri: Uri): String? {
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return cursor.getString(index)
            }
        }
        return uri.lastPathSegment
    }
}

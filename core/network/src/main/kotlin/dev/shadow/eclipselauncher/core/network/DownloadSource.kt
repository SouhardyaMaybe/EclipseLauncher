/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.core.network

import java.io.IOException
import java.io.InputStream

/**
 * Where a downloader gets bytes. The install core owns hashing and storage;
 * this interface only opens a stream. The caller must close it.
 */
fun interface DownloadSource {
    @Throws(IOException::class)
    fun open(url: String): InputStream
}

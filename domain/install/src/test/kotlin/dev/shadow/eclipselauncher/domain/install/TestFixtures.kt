/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.install

import dev.shadow.eclipselauncher.core.common.AppResult
import dev.shadow.eclipselauncher.core.common.Failure
import dev.shadow.eclipselauncher.core.network.DownloadSource
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

internal fun sha1Of(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-1").digest(bytes)
    val digits = "0123456789abcdef"
    return buildString(digest.size * 2) {
        for (byte in digest) {
            val value = byte.toInt() and 0xff
            append(digits[value ushr 4])
            append(digits[value and 0x0f])
        }
    }
}

internal fun downloadItem(
    body: ByteArray,
    destination: String,
    url: String,
    kind: DownloadKind = DownloadKind.LIBRARY,
): DownloadItem = DownloadItem(
    kind = kind,
    url = url,
    sha1 = sha1Of(body),
    size = body.size.toLong(),
    destination = destination,
)

internal class MapSource(private val bodies: Map<String, ByteArray>) : DownloadSource {
    var opens = 0
        private set

    override fun open(url: String): InputStream {
        opens++
        val body = bodies[url] ?: throw IOException("no body for $url")
        return ByteArrayInputStream(body)
    }
}

internal fun <T> AppResult<T>.requireSuccess(): T = when (this) {
    is AppResult.Success -> value
    is AppResult.Error -> error("expected success, got ${failure.code}: ${failure.technicalDetail}")
}

internal fun AppResult<*>.requireFailure(): Failure = when (this) {
    is AppResult.Error -> failure
    is AppResult.Success -> error("expected a failure, got success")
}

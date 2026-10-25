/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.core.network

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.net.ssl.SSLException
import kotlin.random.Random

/**
 * An [HttpURLConnection]-based download source that is safe on Android and
 * testable on the JVM.
 *
 * Redirects are followed manually, one hop at a time, so [guard] sees every
 * host. A 429 or 503 honours `Retry-After`; 5xx responses use exponential
 * backoff with jitter. The connection stays open on success and the caller
 * owns the returned stream.
 */
class HttpDownloadSource(
    private val guard: UrlGuard = StrictUrlGuard(),
    private val connectTimeoutMillis: Int = 15_000,
    private val readTimeoutMillis: Int = 30_000,
    private val maxRedirects: Int = 5,
    private val maxAttempts: Int = 4,
    private val userAgent: String = DEFAULT_USER_AGENT,
    private val sleeper: (Long) -> Unit = { Thread.sleep(it) },
    private val jitterMillis: () -> Long = { Random.nextLong(0L, 250L) },
    private val connectionFactory: (URL) -> HttpURLConnection = { url ->
        url.openConnection() as HttpURLConnection
    },
) : DownloadSource {
    init {
        require(connectTimeoutMillis > 0) { "a connect timeout must be positive" }
        require(readTimeoutMillis > 0) { "a read timeout must be positive" }
        require(maxRedirects >= 0) { "maxRedirects cannot be negative" }
        require(maxAttempts >= 1) { "maxAttempts must be at least one" }
        require(userAgent.isNotBlank()) { "a User-Agent is required" }
    }

    override fun open(url: String): InputStream {
        var current = guard.validate(url)
        var redirects = 0
        var attempts = 0

        while (true) {
            val connection = connect(current)
            val status = try {
                connection.responseCode
            } catch (error: SSLException) {
                connection.disconnect()
                throw NetworkException(
                    code = "NET_TLS",
                    retryable = false,
                    message = "The secure connection failed: ${error.message ?: "TLS error"}",
                    cause = error,
                )
            } catch (error: IOException) {
                connection.disconnect()
                throw NetworkException(
                    code = "NET_IO",
                    retryable = true,
                    message = "The download could not be reached: ${error.message ?: "I/O error"}",
                    cause = error,
                )
            }

            when {
                status in 200..299 -> return streamOf(connection)

                status in 300..399 -> {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    redirects++
                    if (redirects > maxRedirects) {
                        throw NetworkException(
                            code = "NET_REDIRECT_LIMIT",
                            retryable = false,
                            message = "The download redirected more than $maxRedirects times.",
                        )
                    }
                    if (location.isNullOrBlank()) {
                        throw NetworkException(
                            code = "NET_REDIRECT_INVALID",
                            retryable = false,
                            message = "The download redirect has no Location header.",
                        )
                    }
                    current = guard.resolve(current, location)
                }

                status == 429 || status == 503 -> {
                    val retryAfter = retryAfterMillis(connection.getHeaderField("Retry-After"))
                    connection.disconnect()
                    attempts++
                    if (attempts >= maxAttempts) {
                        throw NetworkException(
                            code = "NET_RETRY_EXHAUSTED",
                            retryable = true,
                            message = "The file server is busy after $attempts attempts.",
                        )
                    }
                    sleep(retryAfter ?: backoffMillis(attempts))
                }

                status in 500..599 -> {
                    connection.disconnect()
                    attempts++
                    if (attempts >= maxAttempts) {
                        throw NetworkException(
                            code = "NET_RETRY_EXHAUSTED",
                            retryable = true,
                            message = "The file server failed after $attempts attempts.",
                        )
                    }
                    sleep(backoffMillis(attempts))
                }

                status == 404 || status == 410 -> {
                    connection.disconnect()
                    throw NetworkException(
                        code = "NET_NOT_FOUND",
                        retryable = false,
                        message = "The file is no longer available (HTTP $status).",
                    )
                }

                else -> {
                    connection.disconnect()
                    val retryable = status == 408 || status == 425
                    throw NetworkException(
                        code = "NET_HTTP_STATUS",
                        retryable = retryable,
                        message = "The file server returned HTTP $status.",
                    )
                }
            }
        }
    }

    private fun connect(uri: URI): HttpURLConnection {
        val url = try {
            uri.toURL()
        } catch (error: MalformedURLException) {
            throw NetworkException(
                code = "NET_URL_INVALID",
                retryable = false,
                message = "The download URL is not valid.",
                cause = error,
            )
        } catch (error: IllegalArgumentException) {
            throw NetworkException(
                code = "NET_URL_INVALID",
                retryable = false,
                message = "The download URL is not valid.",
                cause = error,
            )
        }

        val connection = try {
            connectionFactory(url)
        } catch (error: IOException) {
            throw NetworkException(
                code = "NET_IO",
                retryable = true,
                message = "The download could not be started: ${error.message ?: "I/O error"}",
                cause = error,
            )
        } catch (error: IllegalArgumentException) {
            throw NetworkException(
                code = "NET_URL_INVALID",
                retryable = false,
                message = "The download URL is not valid.",
                cause = error,
            )
        }

        return connection.apply {
            instanceFollowRedirects = false
            connectTimeout = connectTimeoutMillis
            readTimeout = readTimeoutMillis
            useCaches = false
            requestMethod = "GET"
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "*/*")
            setRequestProperty("Accept-Encoding", "identity")
        }
    }

    private fun streamOf(connection: HttpURLConnection): InputStream = try {
        connection.inputStream
    } catch (error: SSLException) {
        connection.disconnect()
        throw NetworkException(
            code = "NET_TLS",
            retryable = false,
            message = "The secure connection failed: ${error.message ?: "TLS error"}",
            cause = error,
        )
    } catch (error: IOException) {
        connection.disconnect()
        throw NetworkException(
            code = "NET_IO",
            retryable = true,
            message = "The download body could not be read: ${error.message ?: "I/O error"}",
            cause = error,
        )
    }

    private fun sleep(millis: Long) {
        if (millis <= 0L) return
        try {
            sleeper(millis)
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            throw NetworkException(
                code = "NET_IO",
                retryable = true,
                message = "The download was interrupted.",
                cause = error,
            )
        }
    }

    private fun retryAfterMillis(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        val trimmed = value.trim()

        trimmed.toLongOrNull()?.let { seconds ->
            val capped = seconds.coerceIn(0L, MAX_RETRY_AFTER_MILLIS / 1_000L)
            return capped * 1_000L
        }

        return try {
            val instant = ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME)
                .toInstant()
            (instant.toEpochMilli() - System.currentTimeMillis())
                .coerceIn(0L, MAX_RETRY_AFTER_MILLIS)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun backoffMillis(attempt: Int): Long {
        val shift = (attempt - 1).coerceIn(0, 5)
        val base = 1_000L shl shift
        return (base + jitterMillis().coerceAtLeast(0L)).coerceAtMost(MAX_BACKOFF_MILLIS)
    }

    private companion object {
        const val DEFAULT_USER_AGENT =
            "EclipseLauncher/1.0 (+https://github.com/SouhardyaMaybe/EclipseLauncher)"
        const val MAX_RETRY_AFTER_MILLIS = 60_000L
        const val MAX_BACKOFF_MILLIS = 30_000L
    }
}

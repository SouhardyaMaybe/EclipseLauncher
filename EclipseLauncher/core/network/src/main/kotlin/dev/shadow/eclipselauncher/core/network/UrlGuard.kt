/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.core.network

import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

/** Validates a URL before a connection is opened and after every redirect. */
interface UrlGuard {
    @Throws(NetworkException::class)
    fun validate(raw: String): URI

    @Throws(NetworkException::class)
    fun resolve(current: URI, location: String): URI
}

/**
 * The production policy: HTTPS only, no credentials, port 443 only, and an
 * exact allowlisted host. Error messages never echo the URL because it may
 * contain a token in a query string.
 */
class StrictUrlGuard(
    private val allowlist: HostAllowlist = HostAllowlist.Minecraft,
) : UrlGuard {
    override fun validate(raw: String): URI {
        val uri = try {
            URI(raw)
        } catch (error: URISyntaxException) {
            throw NetworkException(
                code = "NET_URL_INVALID",
                retryable = false,
                message = "The download URL is not valid: ${error.reason}",
                cause = error,
            )
        }

        if (uri.scheme?.lowercase(Locale.ROOT) != "https") {
            throw NetworkException(
                code = "NET_URL_INVALID",
                retryable = false,
                message = "The download URL must use HTTPS.",
            )
        }
        val host = uri.host
        if (host.isNullOrBlank()) {
            throw NetworkException(
                code = "NET_URL_INVALID",
                retryable = false,
                message = "The download URL has no host.",
            )
        }
        if (uri.userInfo != null) {
            throw NetworkException(
                code = "NET_URL_INVALID",
                retryable = false,
                message = "The download URL must not contain credentials.",
            )
        }
        if (uri.port != -1 && uri.port != 443) {
            throw NetworkException(
                code = "NET_URL_INVALID",
                retryable = false,
                message = "The download URL must use port 443.",
            )
        }

        val normalisedHost = host.lowercase(Locale.ROOT)
        if (!allowlist.contains(normalisedHost)) {
            throw NetworkException(
                code = "NET_HOST_NOT_ALLOWED",
                retryable = false,
                message = "The download host is not allowed: $normalisedHost",
            )
        }
        return uri
    }

    override fun resolve(current: URI, location: String): URI {
        val resolved = try {
            current.resolve(URI(location))
        } catch (error: URISyntaxException) {
            throw NetworkException(
                code = "NET_REDIRECT_INVALID",
                retryable = false,
                message = "The redirect location is not valid: ${error.reason}",
                cause = error,
            )
        } catch (error: IllegalArgumentException) {
            throw NetworkException(
                code = "NET_REDIRECT_INVALID",
                retryable = false,
                message = "The redirect location is not valid.",
                cause = error,
            )
        }
        return validate(resolved.toString())
    }
}

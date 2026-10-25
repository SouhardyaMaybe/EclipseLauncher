/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.core.network

import com.google.common.truth.Truth.assertThat
import java.net.URI
import org.junit.Assert.assertThrows
import org.junit.Test

class UrlGuardTest {
    private val guard = StrictUrlGuard(HostAllowlist(setOf("example.com")))

    private fun failure(block: () -> Unit): NetworkException =
        assertThrows(NetworkException::class.java) { block() }

    @Test
    fun `accepts an allowlisted HTTPS URL`() {
        val uri = guard.validate("https://example.com/a/b?c=d")

        assertThat(uri.host).isEqualTo("example.com")
        assertThat(uri.scheme).isEqualTo("https")
    }

    @Test
    fun `accepts an explicit 443 port`() {
        val uri = guard.validate("https://example.com:443/file")

        assertThat(uri.port).isEqualTo(443)
    }

    @Test
    fun `matches the host case-insensitively`() {
        val uri = guard.validate("https://EXAMPLE.com/file")

        assertThat(uri.host).isEqualTo("EXAMPLE.com")
    }

    @Test
    fun `rejects cleartext HTTP`() {
        val error = failure { guard.validate("http://example.com/file") }

        assertThat(error.code).isEqualTo("NET_URL_INVALID")
        assertThat(error.retryable).isFalse()
    }

    @Test
    fun `rejects a non-443 port`() {
        val error = failure { guard.validate("https://example.com:8443/file") }

        assertThat(error.code).isEqualTo("NET_URL_INVALID")
    }

    @Test
    fun `rejects credentials in the authority`() {
        val error = failure { guard.validate("https://user:secret@example.com/file") }

        assertThat(error.code).isEqualTo("NET_URL_INVALID")
        assertThat(error.message).doesNotContain("secret")
    }

    @Test
    fun `rejects an unknown host`() {
        val error = failure { guard.validate("https://evil.invalid/file") }

        assertThat(error.code).isEqualTo("NET_HOST_NOT_ALLOWED")
        assertThat(error.retryable).isFalse()
    }

    @Test
    fun `rejects a malformed URL`() {
        val error = failure { guard.validate("not a url") }

        assertThat(error.code).isEqualTo("NET_URL_INVALID")
    }

    @Test
    fun `rejects a relative URL`() {
        val error = failure { guard.validate("/file.jar") }

        assertThat(error.code).isEqualTo("NET_URL_INVALID")
    }

    @Test
    fun `resolves a relative redirect and validates it`() {
        val next = guard.resolve(URI("https://example.com/a/b"), "../c")

        assertThat(next.toString()).isEqualTo("https://example.com/c")
    }

    @Test
    fun `rejects a redirect to a disallowed host`() {
        val error = failure {
            guard.resolve(URI("https://example.com/a"), "https://evil.invalid/file")
        }

        assertThat(error.code).isEqualTo("NET_HOST_NOT_ALLOWED")
    }

    @Test
    fun `rejects a redirect to cleartext`() {
        val error = failure {
            guard.resolve(URI("https://example.com/a"), "http://example.com/file")
        }

        assertThat(error.code).isEqualTo("NET_URL_INVALID")
    }

    @Test
    fun `rejects a malformed redirect location`() {
        val error = failure {
            guard.resolve(URI("https://example.com/a"), "http://[")
        }

        assertThat(error.code).isEqualTo("NET_REDIRECT_INVALID")
    }

    @Test
    fun `the official allowlist names the documented hosts`() {
        assertThat(HostAllowlist.Minecraft.contains("piston-data.mojang.com")).isTrue()
        assertThat(HostAllowlist.Minecraft.contains("cdn.modrinth.com")).isTrue()
        assertThat(HostAllowlist.Minecraft.contains("evil.invalid")).isFalse()
    }

    @Test
    fun `an empty allowlist is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { HostAllowlist(emptySet()) }
    }

    @Test
    fun `a blank allowlist entry is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { HostAllowlist(setOf(" ")) }
    }
}

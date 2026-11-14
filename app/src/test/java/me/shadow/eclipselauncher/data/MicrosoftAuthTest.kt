/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for MicrosoftAuth redirect parsing and authentication flow.
 */
package me.shadow.eclipselauncher.data

import io.ktor.client.mock.MockEngine
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class MicrosoftAuthTest {

    private val client = HttpClient(MockEngine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
    private val auth = MicrosoftAuth(NetworkClient(client))

    @Test
    fun testParseRedirectDesktopRedirectWithCode() {
        val url = "https://login.live.com/oauth20_desktop.srf?code=test-code-123"
        val result = auth.parseRedirect(url)
        assertNotNull(result)
        assertTrue(result is RedirectResult.Success)
        assertEquals("test-code-123", (result as RedirectResult.Success).code)
    }

    @Test
    fun testParseRedirectCustomSchemeRedirectWithCode() {
        // This is the actual redirect URL Microsoft sends for client ID 00000000402b5328
        val url = "ms-xal-00000000402b5328://auth/?code=test-code-456"
        val result = auth.parseRedirect(url)
        assertNotNull(result)
        assertTrue(result is RedirectResult.Success)
        assertEquals("test-code-456", (result as RedirectResult.Success).code)
    }

    @Test
    fun testParseRedirectCustomSchemeRedirectWithCodeAndFragment() {
        // Custom scheme redirect with a fragment (some OAuth providers add state)
        val url = "ms-xal-00000000402b5328://auth/?code=test-code-789#fragment"
        val result = auth.parseRedirect(url)
        assertNotNull(result)
        assertTrue(result is RedirectResult.Success)
        assertEquals("test-code-789", (result as RedirectResult.Success).code)
    }

    @Test
    fun testParseRedirectDesktopRedirectWithError() {
        val url = "https://login.live.com/oauth20_desktop.srf?error=access_denied"
        val result = auth.parseRedirect(url)
        assertNotNull(result)
        assertTrue(result is RedirectResult.Denied)
        assertEquals("access_denied", (result as RedirectResult.Denied).error)
    }

    @Test
    fun testParseRedirectCustomSchemeRedirectWithError() {
        val url = "ms-xal-00000000402b5328://auth/?error=access_denied"
        val result = auth.parseRedirect(url)
        assertNotNull(result)
        assertTrue(result is RedirectResult.Denied)
        assertEquals("access_denied", (result as RedirectResult.Denied).error)
    }

    @Test
    fun testParseRedirectCustomSchemeRedirectWithErrorAndDescription() {
        val url = "ms-xal-00000000402b5328://auth/?error=server_error&error_description=Server%20is%20down"
        val result = auth.parseRedirect(url)
        assertNotNull(result)
        assertTrue(result is RedirectResult.Failure)
        assertEquals("server_error", (result as RedirectResult.Failure).error)
        assertEquals("Server is down", (result as RedirectResult.Failure).description)
    }

    @Test
    fun testParseRedirectUnrelatedUrlReturnsNull() {
        val url = "https://example.com/some/path"
        val result = auth.parseRedirect(url)
        assertNull(result)
    }

    @Test
    fun testParseRedirectEmptyUrlReturnsNull() {
        val result = auth.parseRedirect("")
        assertNull(result)
    }

    @Test
    fun testParseRedirectCustomSchemeWithoutQueryReturnsNull() {
        val url = "ms-xal-00000000402b5328://auth/"
        val result = auth.parseRedirect(url)
        assertNull(result)
    }

    @Test
    fun testParseRedirectCustomSchemeWithErrorNoDescription() {
        val url = "ms-xal-00000000402b5328://auth/?error=invalid_request"
        val result = auth.parseRedirect(url)
        assertNotNull(result)
        assertTrue(result is RedirectResult.Failure)
        assertEquals("invalid_request", (result as RedirectResult.Failure).error)
        assertNull((result as RedirectResult.Failure).description)
    }

    @Test
    fun testParseRedirectDesktopRedirectWithCodeAndState() {
        val url = "https://login.live.com/oauth20_desktop.srf?code=test-code&state=xyz"
        val result = auth.parseRedirect(url)
        assertNotNull(result)
        assertTrue(result is RedirectResult.Success)
        assertEquals("test-code", (result as RedirectResult.Success).code)
    }
}

class MicrosoftAuthFlowTest {

    private val testDispatcher = TestCoroutineDispatcher()

    @Test
    fun testLoginFlowSuccess() = runTest {
        // This test would require a mock HTTP server to test the full chain
        // For now, we verify the redirect parsing works
        val client = HttpClient(MockEngine)
        val auth = MicrosoftAuth(NetworkClient(client))

        val redirectUrl = "ms-xal-00000000402b5328://auth/?code=fake-code"
        val result = auth.parseRedirect(redirectUrl)

        assertNotNull(result)
        assertTrue(result is RedirectResult.Success)
        assertEquals("fake-code", (result as RedirectResult.Success).code)
    }
}
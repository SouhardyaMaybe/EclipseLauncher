/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for MicrosoftAuth redirect parsing and profile parsing.
 * Tests the parseRedirect method and parseMinecraftProfile function.
 */
package me.shadow.eclipselauncher.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MicrosoftAuthTest {

    // parseRedirect is a pure function that doesn't depend on any external state
    // NetworkClient has a no-arg constructor
    private val auth = MicrosoftAuth(NetworkClient())
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testParseRedirectDesktopRedirectWithCode() {
        val url = "https://login.live.com/oauth20_desktop.srf?code=test-code-123"
        val result = auth.parseRedirect(url)
        assertNotNull(result)
        assertTrue(result is RedirectResult.Success)
        assertEquals("test-code-123", (result as RedirectResult.Success).code)
    }

    @Test
    fun testParseMinecraftProfile_ActiveSkinAndCape() {
        // Profile with ACTIVE skin (CLASSIC) and ACTIVE cape
        val response = buildJsonObject {
            put("id", "1234567890abcdef1234567890abcdef")
            put("name", "TestPlayer")
            putJsonArray("skins") {
                putJsonObject {
                    put("id", "skin-1")
                    put("state", "ACTIVE")
                    put("url", "https://textures.minecraft.net/texture/abc123")
                    put("variant", "CLASSIC")
                    put("alias", "Steve")
                }
                putJsonObject {
                    put("id", "skin-2")
                    put("state", "INACTIVE")
                    put("url", "https://textures.minecraft.net/texture/def456")
                    put("variant", "SLIM")
                }
            }
            putJsonArray("capes") {
                putJsonObject {
                    put("id", "cape-1")
                    put("state", "ACTIVE")
                    put("url", "https://textures.minecraft.net/texture/cape123")
                    put("alias", "Minecon2012")
                }
            }
        }
        val profile = parseMinecraftProfile(response)
        assertEquals("TestPlayer", profile.name)
        assertEquals("1234567890abcdef1234567890abcdef", profile.uuid)
        assertNotNull(profile.activeSkin)
        assertEquals("skin-1", profile.activeSkin!!.id)
        assertEquals("CLASSIC", profile.activeSkin!!.variant)
        assertEquals("https://textures.minecraft.net/texture/abc123", profile.activeSkin!!.url)
        assertNotNull(profile.activeCape)
        assertEquals("cape-1", profile.activeCape!!.id)
        assertEquals("https://textures.minecraft.net/texture/cape123", profile.activeCape!!.url)
        assertEquals("Minecon2012", profile.activeCape!!.alias)
    }

    @Test
    fun testParseMinecraftProfile_SlimVariant() {
        // Profile with ACTIVE SLIM skin (Alex model), no cape
        val response = buildJsonObject {
            put("id", "abcdef1234567890abcdef1234567890")
            put("name", "AlexPlayer")
            putJsonArray("skins") {
                putJsonObject {
                    put("id", "skin-slim")
                    put("state", "ACTIVE")
                    put("url", "https://textures.minecraft.net/texture/slim789")
                    put("variant", "SLIM")
                }
            }
            putJsonArray("capes") {
                // No capes
            }
        }
        val profile = parseMinecraftProfile(response)
        assertEquals("AlexPlayer", profile.name)
        assertNotNull(profile.activeSkin)
        assertEquals("SLIM", profile.activeSkin!!.variant)
        assertNull(profile.activeCape)
    }

    @Test
    fun testParseMinecraftProfile_NoCape() {
        // Profile with ACTIVE skin but no cape array
        val response = buildJsonObject {
            put("id", "11111111111111111111111111111111")
            put("name", "NoCapePlayer")
            putJsonArray("skins") {
                putJsonObject {
                    put("id", "skin-nocape")
                    put("state", "ACTIVE")
                    put("url", "https://textures.minecraft.net/texture/nocape111")
                    put("variant", "CLASSIC")
                }
            }
            // No capes field
        }
        val profile = parseMinecraftProfile(response)
        assertNotNull(profile.activeSkin)
        assertNull(profile.activeCape)
    }

    @Test
    fun testParseMinecraftProfile_HttpUrlNormalizedToHttps() {
        // Profile with HTTP URL - should be normalized to HTTPS
        val response = buildJsonObject {
            put("id", "22222222222222222222222222222222")
            put("name", "HttpPlayer")
            putJsonArray("skins") {
                putJsonObject {
                    put("id", "skin-http")
                    put("state", "ACTIVE")
                    put("url", "http://textures.minecraft.net/texture/http123")
                    put("variant", "CLASSIC")
                }
            }
            putJsonArray("capes") {
                putJsonObject {
                    put("id", "cape-http")
                    put("state", "ACTIVE")
                    put("url", "http://textures.minecraft.net/texture/capehttp")
                    put("alias", "HTTP Cape")
                }
            }
        }
        val profile = parseMinecraftProfile(response)
        assertTrue(profile.activeSkin!!.url.startsWith("https://"))
        assertTrue(profile.activeCape!!.url.startsWith("https://"))
        assertEquals("https://textures.minecraft.net/texture/http123", profile.activeSkin!!.url)
        assertEquals("https://textures.minecraft.net/texture/capehttp", profile.activeCape!!.url)
    }

    @Test
    fun testParseMinecraftProfile_MissingArraysHandled() {
        // Profile with missing skins/capes arrays (malformed but shouldn't crash)
        val response = buildJsonObject {
            put("id", "33333333333333333333333333333333")
            put("name", "MinimalPlayer")
            // No skins, no capes
        }
        val profile = parseMinecraftProfile(response)
        assertEquals("MinimalPlayer", profile.name)
        assertNull(profile.activeSkin)
        assertNull(profile.activeCape)
    }

    @Test
    fun testParseMinecraftProfile_NonObjectEntriesIgnored() {
        // Profile with non-object entries in arrays (should be ignored)
        val response = buildJsonObject {
            put("id", "44444444444444444444444444444444")
            put("name", "RobustPlayer")
            putJsonArray("skins") {
                put("not-an-object")
                putJsonObject {
                    put("id", "skin-valid")
                    put("state", "ACTIVE")
                    put("url", "https://textures.minecraft.net/texture/valid123")
                    put("variant", "CLASSIC")
                }
            }
            putJsonArray("capes") {
                put(123)
                putJsonObject {
                    put("id", "cape-valid")
                    put("state", "ACTIVE")
                    put("url", "https://textures.minecraft.net/texture/capevalid")
                    put("alias", "Valid Cape")
                }
            }
        }
        val profile = parseMinecraftProfile(response)
        assertNotNull(profile.activeSkin)
        assertEquals("skin-valid", profile.activeSkin!!.id)
        assertNotNull(profile.activeCape)
        assertEquals("cape-valid", profile.activeCape!!.id)
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
    fun testParseRedirectCustomSchemeWithoutQueryReturnsFailure() {
        val url = "ms-xal-00000000402b5328://auth/"
        val result = auth.parseRedirect(url)
        assertNotNull(result)
        assertTrue(result is RedirectResult.Failure)
        assertEquals("unknown", (result as RedirectResult.Failure).error)
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
/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for MicrosoftAuth redirect parsing and profile parsing.
 * Tests the parseRedirect method and parseMinecraftProfile function.
 */
package me.shadow.eclipselauncher.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    private fun parseJson(jsonString: String): JsonObject =
        json.parseToJsonElement(jsonString).jsonObject

    @Test
    fun testParseMinecraftProfile_ActiveSkinAndCape() {
        // Profile with ACTIVE skin (CLASSIC) and ACTIVE cape
        val response = parseJson("""
            {
                "id": "1234567890abcdef1234567890abcdef",
                "name": "TestPlayer",
                "skins": [
                    {
                        "id": "skin-1",
                        "state": "ACTIVE",
                        "url": "https://textures.minecraft.net/texture/abc123",
                        "variant": "CLASSIC",
                        "alias": "Steve"
                    },
                    {
                        "id": "skin-2",
                        "state": "INACTIVE",
                        "url": "https://textures.minecraft.net/texture/def456",
                        "variant": "SLIM"
                    }
                ],
                "capes": [
                    {
                        "id": "cape-1",
                        "state": "ACTIVE",
                        "url": "https://textures.minecraft.net/texture/cape123",
                        "alias": "Minecon2012"
                    }
                ]
            }
        """)
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
        val response = parseJson("""
            {
                "id": "abcdef1234567890abcdef1234567890",
                "name": "AlexPlayer",
                "skins": [
                    {
                        "id": "skin-slim",
                        "state": "ACTIVE",
                        "url": "https://textures.minecraft.net/texture/slim789",
                        "variant": "SLIM"
                    }
                ],
                "capes": []
            }
        """)
        val profile = parseMinecraftProfile(response)
        assertEquals("AlexPlayer", profile.name)
        assertNotNull(profile.activeSkin)
        assertEquals("SLIM", profile.activeSkin!!.variant)
        assertNull(profile.activeCape)
    }

    @Test
    fun testParseMinecraftProfile_NoCape() {
        // Profile with ACTIVE skin but no cape array
        val response = parseJson("""
            {
                "id": "11111111111111111111111111111111",
                "name": "NoCapePlayer",
                "skins": [
                    {
                        "id": "skin-nocape",
                        "state": "ACTIVE",
                        "url": "https://textures.minecraft.net/texture/nocape111",
                        "variant": "CLASSIC"
                    }
                ]
            }
        """)
        val profile = parseMinecraftProfile(response)
        assertNotNull(profile.activeSkin)
        assertNull(profile.activeCape)
    }

    @Test
    fun testParseMinecraftProfile_HttpUrlNormalizedToHttps() {
        // Profile with HTTP URL - should be normalized to HTTPS
        val response = parseJson("""
            {
                "id": "22222222222222222222222222222222",
                "name": "HttpPlayer",
                "skins": [
                    {
                        "id": "skin-http",
                        "state": "ACTIVE",
                        "url": "http://textures.minecraft.net/texture/http123",
                        "variant": "CLASSIC"
                    }
                ],
                "capes": [
                    {
                        "id": "cape-http",
                        "state": "ACTIVE",
                        "url": "http://textures.minecraft.net/texture/capehttp",
                        "alias": "HTTP Cape"
                    }
                ]
            }
        """)
        val profile = parseMinecraftProfile(response)
        assertTrue(profile.activeSkin!!.url.startsWith("https://"))
        assertTrue(profile.activeCape!!.url.startsWith("https://"))
        assertEquals("https://textures.minecraft.net/texture/http123", profile.activeSkin!!.url)
        assertEquals("https://textures.minecraft.net/texture/capehttp", profile.activeCape!!.url)
    }

    @Test
    fun testParseMinecraftProfile_MissingArraysHandled() {
        // Profile with missing skins/capes arrays (malformed but shouldn't crash)
        val response = parseJson("""
            {
                "id": "33333333333333333333333333333333",
                "name": "MinimalPlayer"
            }
        """)
        val profile = parseMinecraftProfile(response)
        assertEquals("MinimalPlayer", profile.name)
        assertNull(profile.activeSkin)
        assertNull(profile.activeCape)
    }

    @Test
    fun testParseMinecraftProfile_NonObjectEntriesIgnored() {
        // Profile with entries missing required fields (should be ignored by mapNotNull)
        // The parser's mapNotNull filters out entries that don't have all required fields.
        val response = parseJson("""
            {
                "id": "44444444444444444444444444444444",
                "name": "RobustPlayer",
                "skins": [
                    { "state": "ACTIVE" },
                    {
                        "id": "skin-valid",
                        "state": "ACTIVE",
                        "url": "https://textures.minecraft.net/texture/valid123",
                        "variant": "CLASSIC"
                    }
                ],
                "capes": [
                    { "state": "ACTIVE" },
                    {
                        "id": "cape-valid",
                        "state": "ACTIVE",
                        "url": "https://textures.minecraft.net/texture/capevalid",
                        "alias": "Valid Cape"
                    }
                ]
            }
        """)
        val profile = parseMinecraftProfile(response)
        // Valid entries should still be parsed even if some entries are invalid
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
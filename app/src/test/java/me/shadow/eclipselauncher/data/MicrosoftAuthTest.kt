/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for MicrosoftAuth redirect parsing and profile parsing.
 * Tests the parseRedirect method and parseMinecraftProfile function.
 */
package me.shadow.eclipselauncher.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
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

    private fun buildProfileJson(
        id: String,
        name: String,
        skins: List<Map<String, Any?>>,
        capes: List<Map<String, Any?>>,
    ): JsonObject {
        return buildJsonObject {
            put("id", id)
            put("name", name)
            putJsonArray("skins") {
                for (skin in skins) {
                    putJsonObject {
                        skin.forEach { (key, value) ->
                            value?.let { put(key, it) }
                        }
                    }
                }
            }
            putJsonArray("capes") {
                for (cape in capes) {
                    putJsonObject {
                        cape.forEach { (key, value) ->
                            value?.let { put(key, it) }
                        }
                    }
                }
            }
        }
    }

    private fun skinMap(
        id: String,
        state: String,
        url: String,
        variant: String?,
        alias: String? = null,
    ): Map<String, Any?> = mapOf(
        "id" to id,
        "state" to state,
        "url" to url,
        "variant" to variant,
        "alias" to alias,
    )

    private fun capeMap(
        id: String,
        state: String,
        url: String,
        alias: String? = null,
    ): Map<String, Any?> = mapOf(
        "id" to id,
        "state" to state,
        "url" to url,
        "alias" to alias,
    )

    @Test
    fun testParseMinecraftProfile_ActiveSkinAndCape() {
        // Profile with ACTIVE skin (CLASSIC) and ACTIVE cape
        val response = buildProfileJson(
            id = "1234567890abcdef1234567890abcdef",
            name = "TestPlayer",
            skins = listOf(
                skinMap("skin-1", "ACTIVE", "https://textures.minecraft.net/texture/abc123", "CLASSIC", "Steve"),
                skinMap("skin-2", "INACTIVE", "https://textures.minecraft.net/texture/def456", "SLIM"),
            ),
            capes = listOf(
                capeMap("cape-1", "ACTIVE", "https://textures.minecraft.net/texture/cape123", "Minecon2012"),
            ),
        )
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
        val response = buildProfileJson(
            id = "abcdef1234567890abcdef1234567890",
            name = "AlexPlayer",
            skins = listOf(
                skinMap("skin-slim", "ACTIVE", "https://textures.minecraft.net/texture/slim789", "SLIM"),
            ),
            capes = emptyList(),
        )
        val profile = parseMinecraftProfile(response)
        assertEquals("AlexPlayer", profile.name)
        assertNotNull(profile.activeSkin)
        assertEquals("SLIM", profile.activeSkin!!.variant)
        assertNull(profile.activeCape)
    }

    @Test
    fun testParseMinecraftProfile_NoCape() {
        // Profile with ACTIVE skin but no cape array
        val response = buildProfileJson(
            id = "11111111111111111111111111111111",
            name = "NoCapePlayer",
            skins = listOf(
                skinMap("skin-nocape", "ACTIVE", "https://textures.minecraft.net/texture/nocape111", "CLASSIC"),
            ),
            capes = emptyList(),
        )
        val profile = parseMinecraftProfile(response)
        assertNotNull(profile.activeSkin)
        assertNull(profile.activeCape)
    }

    @Test
    fun testParseMinecraftProfile_HttpUrlNormalizedToHttps() {
        // Profile with HTTP URL - should be normalized to HTTPS
        val response = buildProfileJson(
            id = "22222222222222222222222222222222",
            name = "HttpPlayer",
            skins = listOf(
                skinMap("skin-http", "ACTIVE", "http://textures.minecraft.net/texture/http123", "CLASSIC"),
            ),
            capes = listOf(
                capeMap("cape-http", "ACTIVE", "http://textures.minecraft.net/texture/capehttp", "HTTP Cape"),
            ),
        )
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
                // Add a non-object element (JsonPrimitive) - should be ignored by mapNotNull
                val arr = JsonArray(listOf(
                    JsonElement(nonObjectValue = true), // This represents a non-object
                    JsonObject(mapOf(
                        "id" to JsonElement("skin-valid"),
                        "state" to JsonElement("ACTIVE"),
                        "url" to JsonElement("https://textures.minecraft.net/texture/valid123"),
                        "variant" to JsonElement("CLASSIC"),
                    )),
                ))
                this@buildJsonObject["skins"] = arr
            }
            putJsonArray("capes") {
                val arr = JsonArray(listOf(
                    JsonElement(nonObjectValue = true),
                    JsonObject(mapOf(
                        "id" to JsonElement("cape-valid"),
                        "state" to JsonElement("ACTIVE"),
                        "url" to JsonElement("https://textures.minecraft.net/texture/capevalid"),
                        "alias" to JsonElement("Valid Cape"),
                    )),
                ))
                this@buildJsonObject["capes"] = arr
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
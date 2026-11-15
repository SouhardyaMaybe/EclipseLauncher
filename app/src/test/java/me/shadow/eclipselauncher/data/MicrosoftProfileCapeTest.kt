/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Pure JVM fixtures for the Microsoft profile cape rules: only the first
 * ACTIVE cape is used, inactive/absent capes mean no cape, and texture URLs
 * are normalized to HTTPS.
 */
package me.shadow.eclipselauncher.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MicrosoftProfileCapeTest {

    private fun profile(capes: String? = null, skins: String? = null): MinecraftProfile {
        val fields = mutableListOf("\"name\":\"CapeUser\"", "\"id\":\"uuid-1\"")
        if (skins != null) fields += "\"skins\":$skins"
        if (capes != null) fields += "\"capes\":$capes"
        val response = Json.parseToJsonElement("{${fields.joinToString(",")}}") as JsonObject
        return parseMinecraftProfile(response)
    }

    @Test
    fun picksTheFirstActiveCapeAndIgnoresInactiveOnes() {
        val parsed = profile(
            capes = """
                [
                  {"id":"inactive","state":"INACTIVE",
                   "url":"http://textures.test/inactive.png","alias":"Old"},
                  {"id":"active","state":"ACTIVE",
                   "url":"http://textures.test/active.png","alias":"Turtle"},
                  {"id":"also-active","state":"ACTIVE",
                   "url":"https://textures.test/second.png"}
                ]
            """.trimIndent(),
        )
        val cape = requireNotNull(parsed.activeCape)
        assertEquals("active", cape.id)
        assertEquals("ACTIVE", cape.state)
        assertEquals("https://textures.test/active.png", cape.url)
        assertEquals("Turtle", cape.alias)
        assertEquals(3, parsed.capes.size)
    }

    @Test
    fun inactiveOnlyProfileHasNoActiveCape() {
        val parsed = profile(
            capes = """
                [
                  {"id":"inactive","state":"INACTIVE",
                   "url":"https://textures.test/inactive.png"}
                ]
            """.trimIndent(),
        )
        assertNull(parsed.activeCape)
        assertEquals(1, parsed.capes.size)
    }

    @Test
    fun profileWithoutCapesHasNoActiveCape() {
        val parsed = profile()
        assertTrue(parsed.capes.isEmpty())
        assertNull(parsed.activeCape)
    }

    @Test
    fun emptyCapeArrayHasNoActiveCape() {
        val parsed = profile(capes = "[]")
        assertTrue(parsed.capes.isEmpty())
        assertNull(parsed.activeCape)
    }

    @Test
    fun activeCapeUrlIsNormalizedToHttps() {
        val parsed = profile(
            capes = """
                [
                  {"id":"cape","state":"ACTIVE",
                   "url":"http://textures.minecraft.net/texture/abc"}
                ]
            """.trimIndent(),
        )
        assertEquals(
            "https://textures.minecraft.net/texture/abc",
            parsed.activeCape?.url,
        )
    }

    @Test
    fun normalizeTextureUrlOnlyRewritesHttp() {
        assertEquals(
            "https://textures.minecraft.net/texture/abc",
            normalizeTextureUrl("http://textures.minecraft.net/texture/abc"),
        )
        assertEquals(
            "https://textures.minecraft.net/texture/abc",
            normalizeTextureUrl("https://textures.minecraft.net/texture/abc"),
        )
        assertEquals("", normalizeTextureUrl(""))
        assertEquals("ftp://textures.test/cape.png", normalizeTextureUrl("ftp://textures.test/cape.png"))
    }

    @Test
    fun activeSkinStillParsesWithVariant() {
        val parsed = profile(
            skins = """
                [
                  {"id":"classic","state":"INACTIVE",
                   "url":"https://textures.test/classic.png","variant":"CLASSIC"},
                  {"id":"slim","state":"ACTIVE",
                   "url":"https://textures.test/slim.png","variant":"SLIM"}
                ]
            """.trimIndent(),
        )
        assertEquals("slim", parsed.activeSkin?.id)
        assertEquals("SLIM", parsed.activeSkin?.variant)
    }

    @Test
    fun nonObjectEntriesAreIgnoredInsteadOfCrashing() {
        val parsed = profile(
            capes = """
                [
                  "not-a-cape",
                  {"id":"active","state":"ACTIVE",
                   "url":"https://textures.test/active.png"}
                ]
            """.trimIndent(),
        )
        assertEquals(1, parsed.capes.size)
        assertEquals("active", parsed.activeCape?.id)
    }
}

/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.mojang

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.JsonArray
import org.junit.Assert.assertThrows
import org.junit.Test

class ArgumentTemplateTest {
    @Test
    fun `parses literals and conditional entries`() {
        val raw = MojangJson.parseToJsonElement(
            """
            [
              "--username",
              {
                "rules": [{"action": "allow", "features": {"is_demo_user": true}}],
                "value": ["--demo", "true"]
              }
            ]
            """.trimIndent(),
        ) as JsonArray

        val entries = ArgumentTemplate.parse(raw)

        assertThat(entries).hasSize(2)
        assertThat(entries[0]).isEqualTo(ArgumentEntry.Literal("--username"))
        assertThat(entries[1]).isEqualTo(
            ArgumentEntry.Conditional(
                rules = listOf(
                    Rule(action = "allow", features = mapOf("is_demo_user" to true)),
                ),
                values = listOf("--demo", "true"),
            ),
        )
    }

    @Test
    fun `expands only matching conditional entries`() {
        val entries = listOf(
            ArgumentEntry.Literal("--version"),
            ArgumentEntry.Literal("\${version_name}"),
            ArgumentEntry.Conditional(
                rules = listOf(Rule(action = "allow", features = mapOf("demo" to true))),
                values = listOf("--demo"),
            ),
        )

        val result = ArgumentTemplate.expand(
            entries = entries,
            context = LaunchContext(features = mapOf("demo" to false)),
            values = mapOf("version_name" to "26.3"),
        )

        assertThat(result).containsExactly("--version", "26.3").inOrder()
    }

    @Test
    fun `an unknown macro fails instead of reaching the game`() {
        val entries = listOf(ArgumentEntry.Literal("\${auth_access_token}"))

        assertThrows(MetadataException::class.java) {
            ArgumentTemplate.expand(entries, LaunchContext(), values = emptyMap())
        }
    }

    @Test
    fun `a macro can appear inside a larger value`() {
        val expanded = ArgumentTemplate.expandMacros(
            "-Djava.library.path=\${natives_directory}",
            mapOf("natives_directory" to "/data/app/lib/arm64"),
        )

        assertThat(expanded).isEqualTo("-Djava.library.path=/data/app/lib/arm64")
    }

    @Test
    fun `legacy arguments keep quoted paths together`() {
        val entries = ArgumentTemplate.legacy(
            "--username Player --gameDir \"/storage/emulated/0/My Games\"",
        )

        assertThat(ArgumentTemplate.expand(entries, LaunchContext(), emptyMap()))
            .containsExactly(
                "--username",
                "Player",
                "--gameDir",
                "/storage/emulated/0/My Games",
            ).inOrder()
    }

    @Test
    fun `an unterminated legacy quote fails`() {
        assertThrows(MetadataException::class.java) {
            ArgumentTemplate.legacy("--gameDir \"unfinished")
        }
    }
}

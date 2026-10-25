/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.mojang

import com.google.common.truth.Truth.assertThat
import dev.shadow.eclipselauncher.core.common.AppResult
import org.junit.Test

class MojangMetadataTest {
    @Test
    fun `parses the official manifest shape`() {
        val result = MojangMetadata.parseManifest(
            """
            {
              "latest": {"release": "26.3", "snapshot": "26.4-snapshot-5"},
              "versions": [
                {
                  "id": "26.3",
                  "type": "release",
                  "url": "https://piston-meta.mojang.com/v1/packages/abc/26.3.json",
                  "sha1": "0123456789abcdef0123456789abcdef01234567",
                  "releaseTime": "2026-09-15T10:00:00+00:00"
                }
              ]
            }
            """.trimIndent(),
        )

        val manifest = when (result) {
            is AppResult.Success -> result.value
            is AppResult.Error -> error(result.failure.technicalDetail ?: "parse failed")
        }
        assertThat(manifest.latest.release).isEqualTo("26.3")
        assertThat(MojangMetadata.latestRelease(manifest)?.id).isEqualTo("26.3")
        assertThat(MojangMetadata.findVersion(manifest, "26.4-snapshot-5")).isNull()
    }

    @Test
    fun `parses a version with modern arguments and a library`() {
        val result = MojangMetadata.parseVersion(
            """
            {
              "id": "26.3",
              "type": "release",
              "mainClass": "net.minecraft.client.main.Main",
              "assets": "34",
              "assetIndex": {
                "id": "34",
                "sha1": "0123456789abcdef0123456789abcdef01234567",
                "size": 597035,
                "totalSize": 484894038,
                "url": "https://piston-meta.mojang.com/v1/packages/abc/34.json"
              },
              "downloads": {
                "client": {
                  "sha1": "0123456789abcdef0123456789abcdef01234567",
                  "size": 41483720,
                  "url": "https://piston-data.mojang.com/v1/objects/abc/client.jar"
                }
              },
              "libraries": [
                {
                  "name": "org.lwjgl:lwjgl:3.4.3",
                  "downloads": {
                    "artifact": {
                      "path": "org/lwjgl/lwjgl/3.4.3/lwjgl-3.4.3.jar",
                      "sha1": "0123456789abcdef0123456789abcdef01234567",
                      "size": 100,
                      "url": "https://libraries.minecraft.net/org/lwjgl/lwjgl/3.4.3/lwjgl-3.4.3.jar"
                    }
                  }
                }
              ],
              "arguments": {
                "game": ["--username", "${'$'}{auth_player_name}"],
                "jvm": [
                  {"rules": [{"action": "allow", "os": {"name": "linux"}}], "value": "-Dfoo=bar"}
                ]
              }
            }
            """.trimIndent(),
        )

        val version = when (result) {
            is AppResult.Success -> result.value
            is AppResult.Error -> error(result.failure.technicalDetail ?: "parse failed")
        }
        assertThat(version.mainClass).isEqualTo("net.minecraft.client.main.Main")
        assertThat(version.client?.size).isEqualTo(41_483_720L)
        assertThat(version.libraries).hasSize(1)
        assertThat(version.arguments?.game?.size).isEqualTo(2)
    }

    @Test
    fun `malformed JSON becomes a resolve failure`() {
        val result = MojangMetadata.parseVersion("{not json")

        assertThat(result).isInstanceOf(AppResult.Error::class.java)
        val failure = (result as AppResult.Error).failure
        assertThat(failure.code).isEqualTo("MOJANG_METADATA_INVALID")
    }
}

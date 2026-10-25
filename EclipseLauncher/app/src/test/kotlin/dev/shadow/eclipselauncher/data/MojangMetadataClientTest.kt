/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.data

import com.google.common.truth.Truth.assertThat
import dev.shadow.eclipselauncher.core.common.AppResult
import dev.shadow.eclipselauncher.core.common.Failure
import dev.shadow.eclipselauncher.core.common.FailureStage
import dev.shadow.eclipselauncher.core.common.UserAction
import dev.shadow.eclipselauncher.core.network.DownloadSource
import dev.shadow.eclipselauncher.core.network.NetworkException
import java.io.ByteArrayInputStream
import java.io.InputStream
import org.junit.Test

/**
 * The metadata client is the boundary between the strict HTTP source and the
 * pure-JVM parser. These tests pin its three jobs: transport, size caps and
 * stable failure codes. No Android type is involved, so they run as ordinary
 * JVM tests.
 */
class MojangMetadataClientTest {
    private val manifestUrl =
        "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    private val versionUrl =
        "https://piston-meta.mojang.com/v1/packages/abc/26.3.json"
    private val indexUrl =
        "https://piston-meta.mojang.com/v1/packages/def/34.json"

    private val manifestJson = """
        {
          "latest": {"release": "26.3", "snapshot": "26.4-snapshot-5"},
          "versions": [
            {
              "id": "26.3",
              "type": "release",
              "url": "$versionUrl",
              "sha1": "0123456789abcdef0123456789abcdef01234567",
              "releaseTime": "2026-09-15T10:00:00+00:00"
            }
          ]
        }
    """.trimIndent()

    private val versionJson = """
        {
          "id": "26.3",
          "type": "release",
          "mainClass": "net.minecraft.client.main.Main",
          "assets": "34",
          "assetIndex": {
            "id": "34",
            "sha1": "0123456789abcdef0123456789abcdef01234567",
            "size": 597035,
            "url": "$indexUrl"
          },
          "downloads": {
            "client": {
              "sha1": "0123456789abcdef0123456789abcdef01234567",
              "size": 41483720,
              "url": "https://piston-data.mojang.com/v1/objects/abc/client.jar"
            }
          },
          "libraries": []
        }
    """.trimIndent()

    private val indexJson = """
        {
          "objects": {
            "minecraft/sounds/example.ogg": {
              "hash": "0123456789abcdef0123456789abcdef01234567",
              "size": 12
            }
          }
        }
    """.trimIndent()

    @Test
    fun `parses the official manifest and caches it`() {
        var opens = 0
        val source = DownloadSource { url ->
            opens++
            assertThat(url).isEqualTo(manifestUrl)
            ByteArrayInputStream(manifestJson.toByteArray())
        }
        val client = MojangMetadataClient(source)

        val first = client.fetchManifest().requireSuccess()
        val second = client.fetchManifest().requireSuccess()

        assertThat(first.latest.release).isEqualTo("26.3")
        assertThat(second.versions.map { it.id }).containsExactly("26.3")
        assertThat(opens).isEqualTo(1)
    }

    @Test
    fun `rejects malformed manifest json with a stable code`() {
        val client = MojangMetadataClient(DownloadSource {
            ByteArrayInputStream("{\"latest\":".toByteArray())
        })

        val failure = client.fetchManifest().requireFailure()

        assertThat(failure.code).isEqualTo("MOJANG_METADATA_INVALID")
        assertThat(failure.stage).isEqualTo(FailureStage.RESOLVE)
        assertThat(failure.retryable).isFalse()
    }

    @Test
    fun `rejects a manifest larger than its cap`() {
        val client = MojangMetadataClient(DownloadSource { EndlessStream() })

        val failure = client.fetchManifest().requireFailure()

        assertThat(failure.code).isEqualTo("MOJANG_METADATA_INVALID")
        assertThat(failure.userMessage).contains("too large")
    }

    @Test
    fun `maps a network failure to its stable code and stage`() {
        val client = MojangMetadataClient(DownloadSource {
            throw NetworkException("NET_NOT_FOUND", retryable = false, message = "gone")
        })

        val failure = client.fetchManifest().requireFailure()

        assertThat(failure.code).isEqualTo("NET_NOT_FOUND")
        assertThat(failure.stage).isEqualTo(FailureStage.NETWORK)
        assertThat(failure.retryable).isFalse()
        assertThat(failure.userMessage).contains("no longer available")
    }

    @Test
    fun `stops on cancellation without consuming the body`() {
        var cancellationChecks = 0
        val client = MojangMetadataClient(DownloadSource {
            ByteArrayInputStream(ByteArray(1024) { ' '.code.toByte() })
        })

        val failure = client.fetchManifest {
            cancellationChecks++
            cancellationChecks > 1
        }.requireFailure()

        assertThat(failure.code).isEqualTo("INSTALL_CANCELLED")
        assertThat(failure.userAction).isEqualTo(UserAction.NONE)
    }

    @Test
    fun `parses a version document through the transport boundary`() {
        val client = MojangMetadataClient(DownloadSource { url ->
            assertThat(url).isEqualTo(versionUrl)
            ByteArrayInputStream(versionJson.toByteArray())
        })

        val version = client.fetchVersion(versionUrl).requireSuccess()

        assertThat(version.id).isEqualTo("26.3")
        assertThat(version.mainClass).isEqualTo("net.minecraft.client.main.Main")
        assertThat(version.client?.size).isEqualTo(41_483_720L)
    }

    @Test
    fun `parses an asset index through the transport boundary`() {
        val client = MojangMetadataClient(DownloadSource { url ->
            assertThat(url).isEqualTo(indexUrl)
            ByteArrayInputStream(indexJson.toByteArray())
        })

        val index = client.fetchAssetIndex(indexUrl).requireSuccess()

        assertThat(index.objects).hasSize(1)
        assertThat(index.objects.getValue("minecraft/sounds/example.ogg").size)
            .isEqualTo(12L)
    }

    private class EndlessStream : InputStream() {
        override fun read(): Int = ' '.code

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            buffer.fill(' '.code.toByte(), offset, offset + length)
            return length
        }
    }

    private fun <T> AppResult<T>.requireSuccess(): T = when (this) {
        is AppResult.Success -> value
        is AppResult.Error -> throw AssertionError(
            "expected success, got ${failure.code}: ${failure.technicalDetail}",
        )
    }

    private fun AppResult<*>.requireFailure(): Failure = when (this) {
        is AppResult.Error -> failure
        is AppResult.Success -> throw AssertionError("expected a failure, got success")
    }
}

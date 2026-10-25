/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.install

import com.google.common.truth.Truth.assertThat
import dev.shadow.eclipselauncher.core.common.FailureStage
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ArtifactStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val destination = "libraries/com/example/lib/1.0/lib-1.0.jar"
    private val url = "https://example.invalid/lib-1.0.jar"

    private fun newStore(): ArtifactStore =
        ArtifactStore(temporaryFolder.newFolder("store").toPath())

    private fun item(
        body: ByteArray,
        destination: String = this.destination,
        url: String = this.url,
        sha1: String = sha1Hex(body),
        size: Long = body.size.toLong(),
    ) = DownloadItem(
        kind = DownloadKind.LIBRARY,
        url = url,
        sha1 = sha1,
        size = size,
        destination = destination,
    )

    private fun partOf(store: ArtifactStore, destination: String = this.destination): Path {
        val file = store.pathFor(destination)
        return file.resolveSibling(file.fileName.toString() + ".part")
    }

    @Test
    fun `stores a new object and verifies it`() {
        val store = newStore()
        val body = "hello".toByteArray()
        val source = FakeSource(mapOf(url to body))

        val written = store.store(item(body), source)

        assertThat(written).isEqualTo(5)
        assertThat(source.opens).isEqualTo(1)
        assertThat(Files.readAllBytes(store.pathFor(destination))).isEqualTo(body)
        assertThat(store.isVerified(item(body))).isTrue()
        assertThat(Files.exists(partOf(store))).isFalse()
    }

    @Test
    fun `verifies a SHA-1 that contains high-bit bytes`() {
        val store = newStore()
        val body = "hello".toByteArray()
        val known = "aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d"

        assertThat(sha1Hex(body)).isEqualTo(known)
        store.store(item(body, sha1 = known), FakeSource(mapOf(url to body)))

        assertThat(store.isVerified(item(body, sha1 = known))).isTrue()
    }

    @Test
    fun `a verified object is not fetched again`() {
        val store = newStore()
        val body = "hello".toByteArray()
        val source = FakeSource(mapOf(url to body))
        store.store(item(body), source)

        val written = store.store(item(body), source)

        assertThat(written).isEqualTo(0)
        assertThat(source.opens).isEqualTo(1)
    }

    @Test
    fun `a digest mismatch removes the object and the partial file`() {
        val store = newStore()
        val body = "hello".toByteArray()
        val bad = item(body, sha1 = "0".repeat(40))

        val error = assertThrows(StoreException::class.java) {
            store.store(bad, FakeSource(mapOf(url to body)))
        }

        assertThat(error.code).isEqualTo("DOWNLOAD_HASH_MISMATCH")
        assertThat(error.stage).isEqualTo(FailureStage.VERIFY)
        assertThat(error.retryable).isTrue()
        assertThat(Files.exists(store.pathFor(destination))).isFalse()
        assertThat(Files.exists(partOf(store))).isFalse()
    }

    @Test
    fun `a short body is a size mismatch`() {
        val store = newStore()
        val body = "hello".toByteArray()

        val error = assertThrows(StoreException::class.java) {
            store.store(item(body, size = 10), FakeSource(mapOf(url to body)))
        }

        assertThat(error.code).isEqualTo("DOWNLOAD_SIZE_MISMATCH")
        assertThat(Files.exists(store.pathFor(destination))).isFalse()
        assertThat(Files.exists(partOf(store))).isFalse()
    }

    @Test
    fun `an oversized body is rejected before it is accepted`() {
        val store = newStore()
        val body = "hello".toByteArray()

        val error = assertThrows(StoreException::class.java) {
            store.store(item(body, size = 4), FakeSource(mapOf(url to body)))
        }

        assertThat(error.code).isEqualTo("DOWNLOAD_SIZE_MISMATCH")
        assertThat(Files.exists(store.pathFor(destination))).isFalse()
        assertThat(Files.exists(partOf(store))).isFalse()
    }

    @Test
    fun `a traversal destination is rejected before the source is opened`() {
        val store = newStore()
        val body = "hello".toByteArray()
        val source = FakeSource(mapOf(url to body))

        val error = assertThrows(StoreException::class.java) {
            store.store(item(body, destination = "../escape.bin"), source)
        }

        assertThat(error.code).isEqualTo("STORAGE_PATH_INVALID")
        assertThat(source.opens).isEqualTo(0)
        assertThat(Files.exists(temporaryFolder.root.toPath().resolve("escape.bin"))).isFalse()
    }

    @Test
    fun `cancellation between chunks leaves no partial file`() {
        val store = newStore()
        val body = ByteArray(300_000) { it.toByte() }
        var checks = 0

        val error = assertThrows(StoreException::class.java) {
            store.store(
                item = item(body),
                source = FakeSource(mapOf(url to body)),
                isCancelled = {
                    checks++
                    checks > 2
                },
            )
        }

        assertThat(error.code).isEqualTo("INSTALL_CANCELLED")
        assertThat(error.stage).isEqualTo(FailureStage.INSTALL)
        assertThat(Files.exists(store.pathFor(destination))).isFalse()
        assertThat(Files.exists(partOf(store))).isFalse()
    }

    @Test
    fun `a zero byte object is stored and verified`() {
        val store = newStore()
        val body = ByteArray(0)
        val source = FakeSource(mapOf(url to body))

        store.store(item(body), source)

        assertThat(store.isVerified(item(body))).isTrue()
        assertThat(Files.size(store.pathFor(destination))).isEqualTo(0)
    }

    @Test
    fun `quarantine moves a corrupt object aside`() {
        val store = newStore()
        val body = "hello".toByteArray()
        val file = store.pathFor(destination)
        Files.createDirectories(file.parent)
        Files.write(file, "corrupt".toByteArray())

        val bad = store.quarantine(item(body))

        assertThat(bad).isNotNull()
        assertThat(Files.exists(file)).isFalse()
        assertThat(Files.readAllBytes(bad!!)).isEqualTo("corrupt".toByteArray())
    }

    @Test
    fun `quarantine is a no-op when the object is absent`() {
        val store = newStore()

        assertThat(store.quarantine(item("hello".toByteArray()))).isNull()
    }

    private class FakeSource(private val bodies: Map<String, ByteArray>) : DownloadSource {
        var opens = 0
            private set

        override fun open(url: String): InputStream {
            opens++
            val body = bodies[url] ?: throw IOException("no body for $url")
            return ByteArrayInputStream(body)
        }
    }

    private fun sha1Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(bytes)
        val digits = "0123456789abcdef"
        return buildString(digest.size * 2) {
            for (byte in digest) {
                val value = byte.toInt() and 0xff
                append(digits[value ushr 4])
                append(digits[value and 0x0f])
            }
        }
    }
}

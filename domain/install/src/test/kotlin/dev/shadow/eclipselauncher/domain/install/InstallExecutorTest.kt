/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.install

import com.google.common.truth.Truth.assertThat
import dev.shadow.eclipselauncher.core.common.AppResult
import dev.shadow.eclipselauncher.core.common.FailureStage
import dev.shadow.eclipselauncher.core.common.UserAction
import dev.shadow.eclipselauncher.core.network.DownloadSource
import dev.shadow.eclipselauncher.core.network.NetworkException
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InstallExecutorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val clientDestination = "versions/test/client.jar"
    private val libraryDestination = "libraries/com/example/lib/1.0/lib-1.0.jar"

    private fun newStore(): ArtifactStore =
        ArtifactStore(temporaryFolder.newFolder("store").toPath())

    private fun item(
        body: ByteArray,
        destination: String,
        url: String,
        kind: DownloadKind = DownloadKind.LIBRARY,
        sha1: String = sha1Hex(body),
        size: Long = body.size.toLong(),
    ) = DownloadItem(
        kind = kind,
        url = url,
        sha1 = sha1,
        size = size,
        destination = destination,
    )

    private fun planOf(items: List<DownloadItem>): InstallPlan = InstallPlan(
        versionId = "test",
        downloads = items,
        client = items.first(),
        assetIndex = items.getOrElse(1) { items.first() },
        libraries = items.drop(2),
        assets = emptyList(),
        logging = null,
        totalBytes = items.sumOf { it.size },
    )

    private fun partOf(store: ArtifactStore, destination: String): Path {
        val file = store.pathFor(destination)
        return file.resolveSibling(file.fileName.toString() + ".part")
    }

    @Test
    fun `executes every item and reports completed bytes`() {
        val store = newStore()
        val firstBody = "hello".toByteArray()
        val secondBody = "world".toByteArray()
        val first = item(firstBody, clientDestination, "https://example.invalid/client.jar")
        val second = item(secondBody, libraryDestination, "https://example.invalid/lib.jar")
        val progress = mutableListOf<InstallProgress>()

        val result = InstallExecutor(
            store,
            FakeSource(
                mapOf(
                    first.url to firstBody,
                    second.url to secondBody,
                ),
            ),
        ).execute(planOf(listOf(first, second)), onProgress = { progress += it })

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        assertThat(Files.readAllBytes(store.pathFor(clientDestination))).isEqualTo(firstBody)
        assertThat(Files.readAllBytes(store.pathFor(libraryDestination))).isEqualTo(secondBody)

        val last = progress.last()
        assertThat(last.completedItems).isEqualTo(2)
        assertThat(last.totalItems).isEqualTo(2)
        assertThat(last.completedBytes).isEqualTo(10)
        assertThat(last.totalBytes).isEqualTo(10)
        assertThat(last.downloadedBytes).isEqualTo(10)
        assertThat(last.fraction).isEqualTo(1f)
        assertThat(progress.map { it.completedBytes }.zipWithNext().all { (a, b) -> a <= b })
            .isTrue()
    }

    @Test
    fun `a verified item is skipped but still counts as completed`() {
        val store = newStore()
        val firstBody = "hello".toByteArray()
        val secondBody = "world".toByteArray()
        val first = item(firstBody, clientDestination, "https://example.invalid/client.jar")
        val second = item(secondBody, libraryDestination, "https://example.invalid/lib.jar")
        val firstSource = FakeSource(mapOf(first.url to firstBody))
        store.store(first, firstSource)

        val secondSource = FakeSource(mapOf(second.url to secondBody))
        val progress = mutableListOf<InstallProgress>()
        val result = InstallExecutor(store, secondSource)
            .execute(planOf(listOf(first, second)), onProgress = { progress += it })

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        assertThat(secondSource.opens).isEqualTo(1)
        val last = progress.last()
        assertThat(last.completedBytes).isEqualTo(10)
        assertThat(last.downloadedBytes).isEqualTo(secondBody.size.toLong())
        assertThat(last.fraction).isEqualTo(1f)
    }

    @Test
    fun `cancellation stops before the next item`() {
        val store = newStore()
        val firstBody = "hello".toByteArray()
        val secondBody = "world".toByteArray()
        val first = item(firstBody, clientDestination, "https://example.invalid/client.jar")
        val second = item(secondBody, libraryDestination, "https://example.invalid/lib.jar")
        var cancel = false

        val result = InstallExecutor(
            store,
            FakeSource(
                mapOf(
                    first.url to firstBody,
                    second.url to secondBody,
                ),
            ),
        ).execute(
            plan = planOf(listOf(first, second)),
            onProgress = { progress ->
                if (progress.completedItems == 1) cancel = true
            },
            isCancelled = { cancel },
        )

        assertThat(result).isInstanceOf(AppResult.Error::class.java)
        val failure = (result as AppResult.Error).failure
        assertThat(failure.code).isEqualTo("INSTALL_CANCELLED")
        assertThat(failure.stage).isEqualTo(FailureStage.INSTALL)
        assertThat(failure.userAction).isEqualTo(UserAction.NONE)
        assertThat(Files.exists(store.pathFor(clientDestination))).isTrue()
        assertThat(Files.exists(store.pathFor(libraryDestination))).isFalse()
    }

    @Test
    fun `cancellation inside a large object leaves no partial file`() {
        val store = newStore()
        val body = ByteArray(600_000) { it.toByte() }
        val largeItem = item(body, clientDestination, "https://example.invalid/client.jar")
        var cancel = false

        val result = InstallExecutor(store, FakeSource(mapOf(largeItem.url to body))).execute(
            plan = planOf(listOf(largeItem)),
            onProgress = { progress ->
                if (progress.completedItems == 0 && progress.completedBytes > 0) {
                    cancel = true
                }
            },
            isCancelled = { cancel },
        )

        assertThat(result).isInstanceOf(AppResult.Error::class.java)
        assertThat((result as AppResult.Error).failure.code)
            .isEqualTo("INSTALL_CANCELLED")
        assertThat(Files.exists(store.pathFor(clientDestination))).isFalse()
        assertThat(Files.exists(partOf(store, clientDestination))).isFalse()
    }

    @Test
    fun `a store failure stops the plan with its own code`() {
        val store = newStore()
        val firstBody = "hello".toByteArray()
        val secondBody = "world".toByteArray()
        val first = item(firstBody, clientDestination, "https://example.invalid/client.jar")
        val second = item(
            secondBody,
            libraryDestination,
            "https://example.invalid/lib.jar",
            sha1 = "0".repeat(40),
        )

        val result = InstallExecutor(
            store,
            FakeSource(
                mapOf(
                    first.url to firstBody,
                    second.url to secondBody,
                ),
            ),
        ).execute(planOf(listOf(first, second)))

        assertThat(result).isInstanceOf(AppResult.Error::class.java)
        val failure = (result as AppResult.Error).failure
        assertThat(failure.code).isEqualTo("DOWNLOAD_HASH_MISMATCH")
        assertThat(failure.stage).isEqualTo(FailureStage.VERIFY)
        assertThat(failure.retryable).isTrue()
        assertThat(Files.exists(store.pathFor(clientDestination))).isTrue()
        assertThat(Files.exists(store.pathFor(libraryDestination))).isFalse()
    }

    @Test
    fun `a network failure keeps its code and retry advice`() {
        val store = newStore()
        val body = "hello".toByteArray()
        val item = item(body, clientDestination, "https://example.invalid/client.jar")
        val source = DownloadSource {
            throw NetworkException("NET_HTTP_STATUS", true, "the server said no")
        }

        val result = InstallExecutor(store, source).execute(planOf(listOf(item)))

        assertThat(result).isInstanceOf(AppResult.Error::class.java)
        val failure = (result as AppResult.Error).failure
        assertThat(failure.code).isEqualTo("NET_HTTP_STATUS")
        assertThat(failure.stage).isEqualTo(FailureStage.DOWNLOAD)
        assertThat(failure.retryable).isTrue()
        assertThat(failure.userAction).isEqualTo(UserAction.RETRY)
        assertThat(Files.exists(store.pathFor(clientDestination))).isFalse()
        assertThat(Files.exists(partOf(store, clientDestination))).isFalse()
    }

    @Test
    fun `a non-retryable network failure offers no retry`() {
        val store = newStore()
        val body = "hello".toByteArray()
        val item = item(body, clientDestination, "https://example.invalid/client.jar")
        val source = DownloadSource {
            throw NetworkException("NET_NOT_FOUND", false, "the file is gone")
        }

        val result = InstallExecutor(store, source).execute(planOf(listOf(item)))

        val failure = (result as AppResult.Error).failure
        assertThat(failure.code).isEqualTo("NET_NOT_FOUND")
        assertThat(failure.retryable).isFalse()
        assertThat(failure.userAction).isEqualTo(UserAction.NONE)
    }

    @Test
    fun `reports partial bytes while a large object is streaming`() {
        val store = newStore()
        val body = ByteArray(600_000) { it.toByte() }
        val largeItem = item(body, clientDestination, "https://example.invalid/client.jar")
        val progress = mutableListOf<InstallProgress>()

        val result = InstallExecutor(store, FakeSource(mapOf(largeItem.url to body)))
            .execute(planOf(listOf(largeItem)), onProgress = { progress += it })

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        assertThat(
            progress.any {
                it.completedItems == 0 &&
                    it.completedBytes > 0 &&
                    it.completedBytes < it.totalBytes &&
                    it.fraction < 1f
            },
        ).isTrue()
        assertThat(progress.last().fraction).isEqualTo(1f)
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

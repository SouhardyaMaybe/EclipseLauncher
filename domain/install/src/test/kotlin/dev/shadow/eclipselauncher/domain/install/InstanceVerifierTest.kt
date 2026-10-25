/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.install

import com.google.common.truth.Truth.assertThat
import java.nio.file.Files
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InstanceVerifierTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val body = "hello".toByteArray()
    private val path = "libraries/com/example/lib/1.0/lib-1.0.jar"
    private val url = "https://example.invalid/lib.jar"

    private fun newStore(): ArtifactStore =
        ArtifactStore(temporaryFolder.newFolder("store").toPath())

    private fun managed(path: String = this.path): ManagedFile = ManagedFile(
        path = path,
        sha1 = sha1Of(body),
        size = body.size.toLong(),
        kind = DownloadKind.LIBRARY,
        origin = url,
    )

    private fun manifest(files: List<ManagedFile>) = InstanceManifest(
        instanceId = "test-instance",
        versionId = "26.3",
        createdAtEpochMillis = 1,
        planHash = "plan-hash",
        files = files,
    )

    private fun storeBody(store: ArtifactStore) {
        store.store(
            downloadItem(body, path, url),
            MapSource(mapOf(url to body)),
        )
    }

    @Test
    fun `a complete store passes both levels`() {
        val store = newStore()
        storeBody(store)
        val manifest = manifest(listOf(managed()))

        val fast = InstanceVerifier.verify(manifest, store, VerifyLevel.FAST)
        val full = InstanceVerifier.verify(manifest, store, VerifyLevel.FULL)

        assertThat(fast.healthy).isTrue()
        assertThat(fast.checkedFiles).isEqualTo(1)
        assertThat(full.healthy).isTrue()
    }

    @Test
    fun `fast accepts a same-size corruption and full rejects it`() {
        val store = newStore()
        storeBody(store)
        Files.write(store.pathFor(path), "xxxxx".toByteArray())
        val manifest = manifest(listOf(managed()))

        val fast = InstanceVerifier.verify(manifest, store, VerifyLevel.FAST)
        val full = InstanceVerifier.verify(manifest, store, VerifyLevel.FULL)

        assertThat(fast.healthy).isTrue()
        assertThat(full.healthy).isFalse()
        assertThat(full.failures.single().reason).isEqualTo(VerifyFailureReason.DIGEST)
    }

    @Test
    fun `a missing object fails fast verification`() {
        val store = newStore()
        storeBody(store)
        Files.delete(store.pathFor(path))
        val manifest = manifest(listOf(managed()))

        val report = InstanceVerifier.verify(manifest, store, VerifyLevel.FAST)

        assertThat(report.healthy).isFalse()
        assertThat(report.failures.single().reason).isEqualTo(VerifyFailureReason.MISSING)
    }

    @Test
    fun `a size mismatch fails fast verification`() {
        val store = newStore()
        storeBody(store)
        Files.write(store.pathFor(path), "hi".toByteArray())
        val manifest = manifest(listOf(managed()))

        val report = InstanceVerifier.verify(manifest, store, VerifyLevel.FAST)

        assertThat(report.healthy).isFalse()
        assertThat(report.failures.single().reason).isEqualTo(VerifyFailureReason.SIZE)
        assertThat(report.failures.single().detail).contains("expected 5 bytes")
    }

    @Test
    fun `a traversal path in a tampered manifest is rejected`() {
        val store = newStore()
        val manifest = manifest(listOf(managed(path = "../outside.jar")))

        val report = InstanceVerifier.verify(manifest, store, VerifyLevel.FAST)

        assertThat(report.healthy).isFalse()
        assertThat(report.failures.single().reason).isEqualTo(VerifyFailureReason.PATH)
    }

    @Test
    fun `an empty manifest is healthy and checks nothing`() {
        val store = newStore()

        val report = InstanceVerifier.verify(manifest(emptyList()), store, VerifyLevel.FULL)

        assertThat(report.healthy).isTrue()
        assertThat(report.checkedFiles).isEqualTo(0)
    }
}

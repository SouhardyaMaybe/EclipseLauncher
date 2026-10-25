/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.install

import com.google.common.truth.Truth.assertThat
import dev.shadow.eclipselauncher.core.common.EpochClock
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.FileTime
import kotlinx.serialization.encodeToString
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StoreGcTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val now = 1_800_000_000_000L
    private val day = 24L * 60L * 60L * 1000L
    private val clock = EpochClock { now }

    private fun newStore(): ArtifactStore =
        ArtifactStore(temporaryFolder.newFolder("store").toPath())

    private fun newInstances(): Path = temporaryFolder.newFolder("instances").toPath()

    private fun writeInstance(instances: Path, id: String, paths: List<String>) {
        val directory = instances.resolve(id)
        Files.createDirectories(directory)
        val record = InstanceRecord(
            id = id,
            name = id,
            minecraftVersion = "26.3",
            loader = "VANILLA",
            createdAtEpochMillis = now,
            planHash = "plan-$id",
        )
        Files.write(
            directory.resolve("instance.json"),
            InstallJson.encodeToString(record).toByteArray(Charsets.UTF_8),
        )
        val manifest = InstanceManifest(
            instanceId = id,
            versionId = "26.3",
            createdAtEpochMillis = now,
            planHash = "plan-$id",
            files = paths.map { path ->
                ManagedFile(
                    path = path,
                    sha1 = sha1Of("body".toByteArray()),
                    size = 4,
                    kind = DownloadKind.LIBRARY,
                    origin = "https://example.invalid/file.jar",
                )
            },
        )
        Files.write(
            directory.resolve("manifest.json"),
            InstallJson.encodeToString(manifest).toByteArray(Charsets.UTF_8),
        )
    }

    private fun put(
        store: ArtifactStore,
        path: String,
        ageDays: Long,
        body: ByteArray = "body".toByteArray(),
    ): Path {
        val file = store.pathFor(path)
        Files.createDirectories(file.parent)
        Files.write(file, body)
        Files.setLastModifiedTime(file, FileTime.fromMillis(now - ageDays * day))
        return file
    }

    @Test
    fun `collects references from committed instances only`() {
        val store = newStore()
        val instances = newInstances()
        writeInstance(instances, "alpha", listOf("libraries/a.jar", "assets/objects/aa/a"))
        writeInstance(instances, "beta", listOf("versions/26.3/client.jar"))
        writeInstance(instances.resolve(".trash"), "old", listOf("libraries/old.jar"))
        writeInstance(instances.resolve(".staging"), "tx", listOf("libraries/tx.jar"))
        Files.createDirectories(instances.resolve("empty"))

        val references = StoreGc(store, instances, clock).collectReferences().requireSuccess()

        assertThat(references).containsExactly(
            "libraries/a.jar",
            "assets/objects/aa/a",
            "versions/26.3/client.jar",
        )
    }

    @Test
    fun `deletes old unreferenced objects and keeps referenced or young ones`() {
        val store = newStore()
        val instances = newInstances()
        writeInstance(instances, "alpha", listOf("libraries/keep.jar"))
        val keep = put(store, "libraries/keep.jar", ageDays = 10)
        val drop = put(store, "libraries/drop.jar", ageDays = 10)
        val young = put(store, "assets/objects/aa/young", ageDays = 2)

        val report = StoreGc(store, instances, clock).run().requireSuccess()

        assertThat(Files.exists(keep)).isTrue()
        assertThat(Files.exists(drop)).isFalse()
        assertThat(Files.exists(young)).isTrue()
        assertThat(report.scannedFiles).isEqualTo(3)
        assertThat(report.deletedFiles).isEqualTo(1)
        assertThat(report.reclaimedBytes).isEqualTo(4L)
        assertThat(report.keptReferenced).isEqualTo(1)
        assertThat(report.keptYoung).isEqualTo(1)
        assertThat(report.abortedByLock).isFalse()
        assertThat(report.healthy).isTrue()
    }

    @Test
    fun `keeps a pinned in-flight path even when it is old`() {
        val store = newStore()
        val instances = newInstances()
        val client = put(store, "versions/test/client.jar", ageDays = 30)

        val report = StoreGc(store, instances, clock)
            .sweep(emptySet(), pinned = setOf("versions/test/client.jar"))
            .requireSuccess()

        assertThat(Files.exists(client)).isTrue()
        assertThat(report.deletedFiles).isEqualTo(0)
        assertThat(report.keptReferenced).isEqualTo(1)
    }

    @Test
    fun `removes stale partial and quarantine files after the grace period`() {
        val store = newStore()
        val instances = newInstances()
        val stalePart = put(store, "libraries/a.jar.part", ageDays = 8)
        val staleBad = put(store, "libraries/b.jar.bad", ageDays = 8)
        val youngPart = put(store, "libraries/c.jar.part", ageDays = 1)

        val report = StoreGc(store, instances, clock).run().requireSuccess()

        assertThat(Files.exists(stalePart)).isFalse()
        assertThat(Files.exists(staleBad)).isFalse()
        assertThat(Files.exists(youngPart)).isTrue()
        assertThat(report.deletedFiles).isEqualTo(2)
        assertThat(report.keptYoung).isEqualTo(1)
    }

    @Test
    fun `never touches instance trees worlds or logs`() {
        val store = newStore()
        val instances = store.root.resolve("instances")
        val world = put(store, "instances/test/game/saves/world/level.dat", ageDays = 30)
        val latestLog = put(store, "logs/latestlog.txt", ageDays = 30)
        val options = put(store, "options.txt", ageDays = 30)

        val report = StoreGc(store, instances, clock).run().requireSuccess()

        assertThat(Files.exists(world)).isTrue()
        assertThat(Files.exists(latestLog)).isTrue()
        assertThat(Files.exists(options)).isTrue()
        assertThat(report.deletedFiles).isEqualTo(0)
        assertThat(report.scannedFiles).isEqualTo(0)
    }

    @Test
    fun `prunes empty directories but keeps directories with young files`() {
        val store = newStore()
        val instances = newInstances()
        put(store, "libraries/com/example/old.jar", ageDays = 10)
        put(store, "libraries/com/other/young.jar", ageDays = 1)

        StoreGc(store, instances, clock).run().requireSuccess()

        assertThat(Files.exists(store.root.resolve("libraries/com/example"))).isFalse()
        assertThat(Files.exists(store.root.resolve("libraries/com/other"))).isTrue()
        assertThat(Files.exists(store.root.resolve("libraries"))).isTrue()
    }

    @Test
    fun `a malformed manifest aborts collection instead of deleting blindly`() {
        val store = newStore()
        val instances = newInstances()
        val broken = instances.resolve("broken")
        Files.createDirectories(broken)
        Files.write(broken.resolve("instance.json"), "{}".toByteArray())
        Files.write(broken.resolve("manifest.json"), "{not json".toByteArray())

        val failure = StoreGc(store, instances, clock).collectReferences().requireFailure()

        assertThat(failure.code).isEqualTo("STORAGE_GC_REFERENCES_INVALID")
        assertThat(failure.retryable).isFalse()
    }

    @Test
    fun `an instance with a record but no manifest aborts collection`() {
        val store = newStore()
        val instances = newInstances()
        val broken = instances.resolve("broken")
        Files.createDirectories(broken)
        Files.write(broken.resolve("instance.json"), "{}".toByteArray())

        val failure = StoreGc(store, instances, clock).collectReferences().requireFailure()

        assertThat(failure.code).isEqualTo("STORAGE_GC_REFERENCES_INVALID")
    }

    @Test
    fun `an unsafe reference aborts the sweep`() {
        val store = newStore()
        val instances = newInstances()
        val old = put(store, "libraries/drop.jar", ageDays = 10)

        val failure = StoreGc(store, instances, clock)
            .sweep(setOf("../escape"))
            .requireFailure()

        assertThat(failure.code).isEqualTo("STORAGE_GC_REFERENCES_INVALID")
        assertThat(Files.exists(old)).isTrue()
    }

    @Test
    fun `a lock stops the pass without deleting anything`() {
        val store = newStore()
        val instances = newInstances()
        val old = put(store, "libraries/drop.jar", ageDays = 10)

        val report = StoreGc(store, instances, clock)
            .sweep(emptySet(), isLocked = { true })
            .requireSuccess()

        assertThat(report.abortedByLock).isTrue()
        assertThat(report.deletedFiles).isEqualTo(0)
        assertThat(report.scannedFiles).isEqualTo(0)
        assertThat(Files.exists(old)).isTrue()
    }

    @Test
    fun `a symbolic link is skipped and its target is untouched`() {
        val store = newStore()
        val instances = newInstances()
        val outside = temporaryFolder.newFile("outside.bin").toPath()
        Files.write(outside, "outside".toByteArray())
        val link = store.root.resolve("libraries/link.jar")
        Files.createDirectories(link.parent)
        try {
            Files.createSymbolicLink(link, outside)
        } catch (_: UnsupportedOperationException) {
            return
        }
        Files.getFileAttributeView(
            link,
            BasicFileAttributeView::class.java,
            LinkOption.NOFOLLOW_LINKS,
        ).setTimes(FileTime.fromMillis(now - 30 * day), null, null)

        val report = StoreGc(store, instances, clock).run().requireSuccess()

        assertThat(Files.isSymbolicLink(link)).isTrue()
        assertThat(String(Files.readAllBytes(outside), Charsets.UTF_8)).isEqualTo("outside")
        assertThat(report.deletedFiles).isEqualTo(0)
        assertThat(report.scannedFiles).isEqualTo(0)
    }
}

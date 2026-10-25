/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.install

import com.google.common.truth.Truth.assertThat
import dev.shadow.eclipselauncher.core.common.EpochClock
import dev.shadow.eclipselauncher.core.common.FailureStage
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InstanceTransactionTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val clientBody = "client".toByteArray()
    private val indexBody = "index".toByteArray()
    private val libraryBody = "library".toByteArray()
    private val clock = EpochClock { 1_700_000_000_000L }

    private fun newStore(): ArtifactStore =
        ArtifactStore(temporaryFolder.newFolder("store").toPath())

    private fun newInstances(): Path = temporaryFolder.newFolder("instances").toPath()

    private fun items(): List<DownloadItem> = listOf(
        downloadItem(
            body = clientBody,
            destination = "versions/26.3/client.jar",
            url = "https://example.invalid/client.jar",
            kind = DownloadKind.CLIENT,
        ),
        downloadItem(
            body = indexBody,
            destination = "assets/indexes/34.json",
            url = "https://example.invalid/34.json",
            kind = DownloadKind.ASSET_INDEX,
        ),
        downloadItem(
            body = libraryBody,
            destination = "libraries/com/example/lib/1.0/lib-1.0.jar",
            url = "https://example.invalid/lib.jar",
        ),
    )

    private fun bodyFor(item: DownloadItem): ByteArray = when (item.kind) {
        DownloadKind.CLIENT -> clientBody
        DownloadKind.ASSET_INDEX -> indexBody
        else -> libraryBody
    }

    private fun storeAll(store: ArtifactStore, items: List<DownloadItem>) {
        val source = MapSource(items.associate { it.url to bodyFor(it) })
        items.forEach { store.store(it, source) }
    }

    private fun planOf(items: List<DownloadItem>): InstallPlan = InstallPlan(
        versionId = "26.3",
        downloads = items,
        client = items[0],
        assetIndex = items[1],
        libraries = items.drop(2),
        assets = emptyList(),
        logging = null,
        totalBytes = items.sumOf { it.size },
    )

    private fun read(path: Path): String = String(Files.readAllBytes(path), Charsets.UTF_8)

    private fun writeJournal(instances: Path, journal: InstallJournal) {
        Files.write(
            instances.resolve("journal.json"),
            InstallJson.encodeToString(journal).toByteArray(Charsets.UTF_8),
        )
    }

    @Test
    fun `commits a verified plan into an instance tree`() {
        val store = newStore()
        val instances = newInstances()
        val items = items()
        storeAll(store, items)
        val plan = planOf(items)
        val transaction = InstanceTransaction(instances, store, clock)

        val installed = transaction.install(
            id = "test-instance",
            name = "Test Instance",
            minecraftVersion = "26.3",
            loader = "VANILLA",
            plan = plan,
            txid = "tx-1",
        ).requireSuccess()

        assertThat(installed.root).isEqualTo(instances.resolve("test-instance"))
        assertThat(Files.isDirectory(installed.root.resolve("game/saves"))).isTrue()
        assertThat(installed.manifest.files).hasSize(3)
        assertThat(installed.manifest.planHash).isEqualTo(plan.planHash)

        val record = InstallJson.decodeFromString<InstanceRecord>(
            read(installed.root.resolve("instance.json")),
        )
        assertThat(record.id).isEqualTo("test-instance")
        assertThat(record.name).isEqualTo("Test Instance")
        assertThat(record.loader).isEqualTo("VANILLA")
        assertThat(record.planHash).isEqualTo(plan.planHash)

        val decodedPlan = InstallJson.decodeFromString<InstallPlan>(
            read(installed.root.resolve("plan.json")),
        )
        assertThat(decodedPlan.planHash).isEqualTo(plan.planHash)
        assertThat(Files.exists(instances.resolve("journal.json"))).isFalse()
        assertThat(Files.exists(instances.resolve(".staging/tx-1"))).isFalse()
    }

    @Test
    fun `refuses to commit when an object is not verified`() {
        val store = newStore()
        val instances = newInstances()
        val items = items()
        storeAll(store, items.take(2))

        val result = InstanceTransaction(instances, store, clock).install(
            id = "test-instance",
            name = "Test Instance",
            minecraftVersion = "26.3",
            loader = "VANILLA",
            plan = planOf(items),
            txid = "tx-1",
        )

        val failure = result.requireFailure()
        assertThat(failure.code).isEqualTo("INSTALL_INCOMPLETE")
        assertThat(failure.stage).isEqualTo(FailureStage.VERIFY)
        assertThat(failure.retryable).isTrue()
        assertThat(Files.exists(instances.resolve("test-instance"))).isFalse()
    }

    @Test
    fun `rejects an instance id that could escape the root`() {
        val store = newStore()
        val instances = newInstances()
        val items = items()
        storeAll(store, items)

        val result = InstanceTransaction(instances, store, clock).install(
            id = "../escape",
            name = "Escape",
            minecraftVersion = "26.3",
            loader = "VANILLA",
            plan = planOf(items),
            txid = "tx-1",
        )

        assertThat(result.requireFailure().code).isEqualTo("INSTANCE_ID_INVALID")
        assertThat(Files.exists(temporaryFolder.root.toPath().resolve("escape"))).isFalse()
    }

    @Test
    fun `rejects a plan for a different Minecraft version`() {
        val store = newStore()
        val instances = newInstances()

        val result = InstanceTransaction(instances, store, clock).install(
            id = "test-instance",
            name = "Test Instance",
            minecraftVersion = "1.21.1",
            loader = "VANILLA",
            plan = planOf(items()),
            txid = "tx-1",
        )

        assertThat(result.requireFailure().code).isEqualTo("INSTALL_PLAN_INVALID")
    }

    @Test
    fun `replacing an instance keeps the old tree in the trash`() {
        val store = newStore()
        val instances = newInstances()
        val items = items()
        storeAll(store, items)
        val old = instances.resolve("test-instance")
        Files.createDirectories(old)
        Files.write(old.resolve("marker.txt"), "old".toByteArray())

        val installed = InstanceTransaction(instances, store, clock).install(
            id = "test-instance",
            name = "New Name",
            minecraftVersion = "26.3",
            loader = "VANILLA",
            plan = planOf(items),
            txid = "tx-2",
        ).requireSuccess()

        assertThat(Files.exists(installed.root.resolve("marker.txt"))).isFalse()
        val backup = instances.resolve(".trash/test-instance-tx-2/marker.txt")
        assertThat(Files.readAllBytes(backup)).isEqualTo("old".toByteArray())
    }

    @Test
    fun `replacing an instance preserves user content under game`() {
        val store = newStore()
        val instances = newInstances()
        val items = items()
        storeAll(store, items)
        val old = instances.resolve("test-instance")
        Files.createDirectories(old.resolve("game/mods"))
        Files.createDirectories(old.resolve("game/saves/world"))
        Files.write(old.resolve("game/mods/example.jar"), "mod".toByteArray())
        Files.write(old.resolve("game/saves/world/level.dat"), "world".toByteArray())
        Files.write(old.resolve("game/options.txt"), "fov:0.5".toByteArray())

        val installed = InstanceTransaction(instances, store, clock).install(
            id = "test-instance",
            name = "New Name",
            minecraftVersion = "26.3",
            loader = "VANILLA",
            plan = planOf(items),
            txid = "tx-2",
        ).requireSuccess()

        assertThat(
            Files.readAllBytes(installed.root.resolve("game/mods/example.jar")),
        ).isEqualTo("mod".toByteArray())
        assertThat(
            Files.readAllBytes(installed.root.resolve("game/saves/world/level.dat")),
        ).isEqualTo("world".toByteArray())
        assertThat(
            Files.readAllBytes(installed.root.resolve("game/options.txt")),
        ).isEqualTo("fov:0.5".toByteArray())
    }

    @Test
    fun `recovery discards a prepared staging tree`() {
        val store = newStore()
        val instances = newInstances()
        val staging = instances.resolve(".staging/tx-1")
        Files.createDirectories(staging.resolve("game"))
        Files.write(staging.resolve("game/stale.txt"), "stale".toByteArray())
        writeJournal(
            instances,
            InstallJournal(txid = "tx-1", instanceId = "test-instance", state = "prepared"),
        )

        InstanceTransaction(instances, store, clock).recover().requireSuccess()

        assertThat(Files.exists(staging)).isFalse()
        assertThat(Files.exists(instances.resolve("journal.json"))).isFalse()
        assertThat(Files.exists(instances.resolve("test-instance"))).isFalse()
    }

    @Test
    fun `recovery completes a committing rename`() {
        val store = newStore()
        val instances = newInstances()
        val staging = instances.resolve(".staging/tx-1")
        Files.createDirectories(staging)
        Files.write(staging.resolve("instance.json"), "new".toByteArray())
        writeJournal(
            instances,
            InstallJournal(txid = "tx-1", instanceId = "test-instance", state = "committing"),
        )

        InstanceTransaction(instances, store, clock).recover().requireSuccess()

        assertThat(
            String(
                Files.readAllBytes(instances.resolve("test-instance/instance.json")),
                Charsets.UTF_8,
            ),
        ).isEqualTo("new")
        assertThat(Files.exists(staging)).isFalse()
        assertThat(Files.exists(instances.resolve("journal.json"))).isFalse()
    }

    @Test
    fun `recovery restores the backup when the new tree never arrived`() {
        val store = newStore()
        val instances = newInstances()
        val backup = instances.resolve(".trash/test-instance-tx-1")
        Files.createDirectories(backup)
        Files.write(backup.resolve("marker.txt"), "old".toByteArray())
        writeJournal(
            instances,
            InstallJournal(
                txid = "tx-1",
                instanceId = "test-instance",
                state = "committing",
                backup = ".trash/test-instance-tx-1",
            ),
        )

        InstanceTransaction(instances, store, clock).recover().requireSuccess()

        assertThat(
            Files.readAllBytes(instances.resolve("test-instance/marker.txt")),
        ).isEqualTo("old".toByteArray())
        assertThat(Files.exists(backup)).isFalse()
        assertThat(Files.exists(instances.resolve("journal.json"))).isFalse()
    }

    @Test
    fun `recovery removes a committed journal`() {
        val store = newStore()
        val instances = newInstances()
        writeJournal(
            instances,
            InstallJournal(txid = "tx-1", instanceId = "test-instance", state = "committed"),
        )

        InstanceTransaction(instances, store, clock).recover().requireSuccess()

        assertThat(Files.exists(instances.resolve("journal.json"))).isFalse()
    }

    @Test
    fun `recovery refuses an unknown journal state`() {
        val store = newStore()
        val instances = newInstances()
        writeJournal(
            instances,
            InstallJournal(txid = "tx-1", instanceId = "test-instance", state = "mystery"),
        )

        val failure = InstanceTransaction(instances, store, clock).recover().requireFailure()

        assertThat(failure.code).isEqualTo("INSTALL_RECOVERY_FAILED")
        assertThat(Files.exists(instances.resolve("journal.json"))).isTrue()
    }
}

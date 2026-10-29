/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.shadow.eclipselauncher.core.model.InstanceId
import dev.shadow.eclipselauncher.core.model.InstanceSummary
import dev.shadow.eclipselauncher.core.model.LoaderKind
import java.io.File
import java.util.Locale

/**
 * The real, local state of this build.
 *
 * The reference launcher's screens expect an account strip holding more than
 * one saved profile and a small per-instance settings record, so both are
 * persisted here. No field pretends to own a game runtime: the install path is
 * still the one real Vanilla transaction in [InstallCoordinator].
 */
class LauncherState(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val minecraftDir: File = File(context.filesDir, ".minecraft").also { it.mkdirs() }

    var accountName: String by mutableStateOf(
        prefs.getString(KEY_ACCOUNT_NAME, "").orEmpty(),
    )
        private set

    /**
     * The reference account spinner lists "Add account" followed by every
     * saved account. Only local accounts can be created by this build.
     */
    var accounts: List<String> by mutableStateOf(readAccounts())
        private set

    var instance: InstanceSummary? by mutableStateOf(readInstance())
        private set

    /**
     * True when the selected instance has a committed manifest. This is a
     * cheap flag, not a verification: Play starts a repair when it is false,
     * and a full verifier pass can replace it later without changing the UI.
     */
    var instanceInstalled: Boolean by mutableStateOf(false)
        private set

    var curseForgeKey: String by mutableStateOf(
        prefs.getString(KEY_CURSEFORGE, "").orEmpty(),
    )
        private set

    var autoInstallDependencies: Boolean by mutableStateOf(
        prefs.getBoolean(KEY_AUTO_DEPENDENCIES, true),
    )
        private set

    var instanceSharedData: Boolean by mutableStateOf(
        prefs.getBoolean(KEY_INSTANCE_SHARED_DATA, true),
    )
        private set

    var instanceJvmArgs: String by mutableStateOf(
        prefs.getString(KEY_INSTANCE_JVM_ARGS, "").orEmpty(),
    )
        private set

    var instanceRenderer: String by mutableStateOf(
        prefs.getString(KEY_INSTANCE_RENDERER, "").orEmpty(),
    )
        private set

    var instanceRuntime: String by mutableStateOf(
        prefs.getString(KEY_INSTANCE_RUNTIME, "").orEmpty(),
    )
        private set

    var instanceControlmap: String by mutableStateOf(
        prefs.getString(KEY_INSTANCE_CONTROLMAP, "").orEmpty(),
    )
        private set

    init {
        if (!prefs.contains(KEY_ACCOUNTS)) {
            persistAccounts(accounts, accountName)
        }
        if (accounts.isEmpty() && prefs.contains(KEY_ACCOUNTS)) {
            if (accountName.isNotBlank()) {
                accounts = listOf(accountName)
                persistAccounts(accounts, accountName)
            }
        } else if (accounts.isNotEmpty() && accountName !in accounts) {
            accountName = accounts.first()
            prefs.edit().putString(KEY_ACCOUNT_NAME, accountName).apply()
        }
        instanceInstalled = hasManifest(instance)
    }

    fun updateAccountName(value: String) {
        val clean = value.trim()
        if (clean.isBlank()) return
        val previous = accountName
        val updated = accounts.toMutableList()
        val oldIndex = updated.indexOf(previous)
        if (oldIndex >= 0) {
            updated[oldIndex] = clean
        } else if (clean !in updated) {
            updated.add(clean)
        } else {
            selectAccount(clean)
            return
        }
        accounts = updated.distinct()
        accountName = clean
        persistAccounts(accounts, clean)
    }

    fun addLocalAccount(value: String): Boolean {
        val clean = value.trim()
        if (clean.isBlank()) return false
        if (accounts.any { it.equals(clean, ignoreCase = true) }) {
            selectAccount(accounts.first { it.equals(clean, ignoreCase = true) })
            return true
        }
        accounts = accounts + clean
        accountName = clean
        persistAccounts(accounts, clean)
        return true
    }

    fun selectAccount(value: String) {
        if (value !in accounts) return
        accountName = value
        prefs.edit().putString(KEY_ACCOUNT_NAME, value).apply()
    }

    fun deleteAccount(value: String) {
        val updated = accounts.filterNot { it == value }
        accounts = updated
        if (accountName == value) {
            accountName = updated.firstOrNull().orEmpty()
        }
        persistAccounts(updated, accountName)
    }

    /**
     * Creates the one instance this build supports. The reference editor fills
     * a default profile with `latest_release`, which is resolved to a concrete
     * Vanilla id only when the real installer starts.
     */
    fun createInstance(
        loader: LoaderKind,
        name: String = DEFAULT_INSTANCE_NAME,
        minecraftVersion: String = DEFAULT_MINECRAFT_VERSION,
    ): InstanceSummary {
        val cleanName = name.trim().ifBlank { DEFAULT_INSTANCE_NAME }
        val cleanVersion = minecraftVersion.trim().ifBlank { DEFAULT_MINECRAFT_VERSION }
        val summary = InstanceSummary(
            id = InstanceId(slug(cleanName)),
            name = cleanName,
            minecraftVersion = cleanVersion,
            loader = loader,
        )

        prefs.edit()
            .putString(KEY_INSTANCE_ID, summary.id.value)
            .putString(KEY_INSTANCE_NAME, summary.name)
            .putString(KEY_INSTANCE_VERSION, summary.minecraftVersion)
            .putString(KEY_INSTANCE_LOADER, summary.loader.name)
            .putBoolean(KEY_INSTANCE_SHARED_DATA, true)
            .remove(KEY_INSTANCE_JVM_ARGS)
            .remove(KEY_INSTANCE_RENDERER)
            .remove(KEY_INSTANCE_RUNTIME)
            .remove(KEY_INSTANCE_CONTROLMAP)
            .apply()

        instance = summary
        instanceInstalled = false
        instanceSharedData = true
        instanceJvmArgs = ""
        instanceRenderer = ""
        instanceRuntime = ""
        instanceControlmap = ""
        ensureInstanceDirectories(summary)
        LogStore.bootstrapOnce(logsDir(summary))
        return summary
    }

    /**
     * Moves the selected instance root when the editor changes its name, then
     * updates the stored id. The reference renames the directory before it
     * writes the edited instance.
     */
    fun renameInstanceDirectory(newName: String) {
        val current = instance ?: return
        val clean = newName.trim()
        if (clean.isEmpty() || clean == current.name) return

        val oldDir = instanceDir(current)
        val base = slug(clean)
        var candidate = base
        var suffix = 2
        var targetDir = File(minecraftDir, "instances/$candidate")
        while (targetDir.exists() && targetDir.absolutePath != oldDir.absolutePath) {
            candidate = "$base-${suffix++}"
            targetDir = File(minecraftDir, "instances/$candidate")
        }

        if (oldDir.exists() && oldDir.absolutePath != targetDir.absolutePath) {
            if (!oldDir.renameTo(targetDir)) return
        }

        val updated = current.copy(id = InstanceId(candidate))
        val oldLogs = logsDir(current)
        val newLogs = logsDir(updated)
        if (oldLogs.exists() && oldLogs.absolutePath != newLogs.absolutePath) {
            oldLogs.renameTo(newLogs)
        }

        instance = updated
        prefs.edit().putString(KEY_INSTANCE_ID, candidate).apply()
        ensureInstanceDirectories(updated)
    }

    fun updateInstance(name: String, minecraftVersion: String) {
        val current = instance ?: return
        val cleanName = name.trim().ifBlank { current.name }
        val cleanVersion = minecraftVersion.trim().ifBlank { current.minecraftVersion }
        val updated = current.copy(name = cleanName, minecraftVersion = cleanVersion)
        val versionChanged = cleanVersion != current.minecraftVersion

        prefs.edit()
            .putString(KEY_INSTANCE_NAME, updated.name)
            .putString(KEY_INSTANCE_VERSION, updated.minecraftVersion)
            .apply()

        instance = updated
        // Renaming an installed instance does not invalidate its files. A
        // version change does, so Play starts a fresh plan on the next tap.
        if (versionChanged) instanceInstalled = false
        ensureInstanceDirectories(updated)
    }

    fun updateInstanceOptions(
        sharedData: Boolean,
        jvmArgs: String,
        renderer: String,
        runtime: String,
        controlmap: String,
    ) {
        instanceSharedData = sharedData
        instanceJvmArgs = jvmArgs.trim()
        instanceRenderer = renderer
        instanceRuntime = runtime
        instanceControlmap = controlmap
        prefs.edit()
            .putBoolean(KEY_INSTANCE_SHARED_DATA, sharedData)
            .putString(KEY_INSTANCE_JVM_ARGS, instanceJvmArgs)
            .putString(KEY_INSTANCE_RENDERER, instanceRenderer)
            .putString(KEY_INSTANCE_RUNTIME, instanceRuntime)
            .putString(KEY_INSTANCE_CONTROLMAP, instanceControlmap)
            .apply()
    }

    /** Removes the selected profile and every file under its instance root. */
    fun deleteInstance() {
        val current = instance
        if (current != null) {
            instanceDir(current).deleteRecursively()
        }
        prefs.edit()
            .remove(KEY_INSTANCE_ID)
            .remove(KEY_INSTANCE_NAME)
            .remove(KEY_INSTANCE_VERSION)
            .remove(KEY_INSTANCE_LOADER)
            .remove(KEY_INSTANCE_SHARED_DATA)
            .remove(KEY_INSTANCE_JVM_ARGS)
            .remove(KEY_INSTANCE_RENDERER)
            .remove(KEY_INSTANCE_RUNTIME)
            .remove(KEY_INSTANCE_CONTROLMAP)
            .apply()
        instance = null
        instanceInstalled = false
        instanceSharedData = true
        instanceJvmArgs = ""
        instanceRenderer = ""
        instanceRuntime = ""
        instanceControlmap = ""
    }

    /** Called after the transaction commits a manifest for the current instance. */
    fun markInstanceInstalled() {
        instanceInstalled = hasManifest(instance)
    }

    fun updateCurseForgeKey(value: String) {
        val clean = value.trim()
        prefs.edit().putString(KEY_CURSEFORGE, clean).apply()
        curseForgeKey = clean
    }

    fun updateAutoInstallDependencies(value: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_DEPENDENCIES, value).apply()
        autoInstallDependencies = value
    }

    fun instanceDir(summary: InstanceSummary? = instance): File =
        summary?.let { File(minecraftDir, "instances/${it.id.value}") }
            ?: File(minecraftDir, "instances")

    fun logsDir(summary: InstanceSummary? = instance): File =
        summary?.let { File(minecraftDir, "logs/${it.id.value}") }
            ?: File(minecraftDir, "logs")

    fun contentDir(type: ContentType, summary: InstanceSummary? = instance): File =
        File(instanceDir(summary), "game/${type.directory}")

    /** The reference "shared data" switch uses one game directory for all profiles. */
    fun gameDir(summary: InstanceSummary? = instance): File =
        if (instanceSharedData) {
            File(minecraftDir, "game")
        } else {
            File(instanceDir(summary), "game")
        }

    val controlmapDir: File
        get() = File(minecraftDir, "controlmap").also { it.mkdirs() }

    fun ensureInstanceDirectories(summary: InstanceSummary) {
        ContentType.entries.forEach { type ->
            File(instanceDir(summary), "game/${type.directory}").mkdirs()
        }
        logsDir(summary).mkdirs()
        controlmapDir.mkdirs()
    }

    private fun hasManifest(summary: InstanceSummary?): Boolean =
        summary != null && File(instanceDir(summary), "manifest.json").isFile

    private fun readAccounts(): List<String> {
        val stored = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return stored.split(ACCOUNT_SEPARATOR).filter { it.isNotBlank() }
    }

    private fun persistAccounts(values: List<String>, selected: String) {
        prefs.edit()
            .putString(KEY_ACCOUNTS, values.joinToString(ACCOUNT_SEPARATOR))
            .putString(KEY_ACCOUNT_NAME, selected)
            .apply()
    }

    private fun readInstance(): InstanceSummary? {
        val id = prefs.getString(KEY_INSTANCE_ID, null)?.takeIf { it.isNotBlank() } ?: return null
        val name = prefs.getString(KEY_INSTANCE_NAME, null)?.takeIf { it.isNotBlank() } ?: return null
        val version = prefs.getString(KEY_INSTANCE_VERSION, null)?.takeIf { it.isNotBlank() } ?: return null
        val loaderName = prefs.getString(KEY_INSTANCE_LOADER, null) ?: return null
        val loader = runCatching { LoaderKind.valueOf(loaderName) }.getOrNull() ?: return null
        return runCatching {
            InstanceSummary(
                id = InstanceId(id),
                name = name,
                minecraftVersion = version,
                loader = loader,
            )
        }.getOrNull()
    }

    private fun slug(value: String): String {
        val slug = value.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        return slug.ifBlank { "instance" }
    }

    companion object {
        const val DEFAULT_MINECRAFT_VERSION = "latest_release"
        const val DEFAULT_INSTANCE_NAME = "Unnamed"

        private const val PREFS_NAME = "eclipse_launcher"
        private const val ACCOUNT_SEPARATOR = "\u001F"
        private const val KEY_ACCOUNT_NAME = "account.name"
        private const val KEY_ACCOUNTS = "account.list"
        private const val KEY_INSTANCE_ID = "instance.id"
        private const val KEY_INSTANCE_NAME = "instance.name"
        private const val KEY_INSTANCE_VERSION = "instance.version"
        private const val KEY_INSTANCE_LOADER = "instance.loader"
        private const val KEY_INSTANCE_SHARED_DATA = "instance.shared_data"
        private const val KEY_INSTANCE_JVM_ARGS = "instance.jvm_args"
        private const val KEY_INSTANCE_RENDERER = "instance.renderer"
        private const val KEY_INSTANCE_RUNTIME = "instance.runtime"
        private const val KEY_INSTANCE_CONTROLMAP = "instance.controlmap"
        private const val KEY_CURSEFORGE = "curseforge.key"
        private const val KEY_AUTO_DEPENDENCIES = "content.auto_dependencies"
    }
}

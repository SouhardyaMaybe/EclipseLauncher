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
 * There is deliberately no repository layer yet: a single instance and one
 * local account are small enough to persist in SharedPreferences, and every
 * screen reads the same observable state. Game installation, accounts with
 * tokens and the content catalogue will replace pieces of this class behind
 * the same properties.
 */
class LauncherState(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val minecraftDir: File = File(context.filesDir, ".minecraft").also { it.mkdirs() }

    var accountName: String by mutableStateOf(
        prefs.getString(KEY_ACCOUNT_NAME, DEFAULT_ACCOUNT_NAME) ?: DEFAULT_ACCOUNT_NAME,
    )
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

    init {
        instanceInstalled = hasManifest(instance)
    }

    fun updateAccountName(value: String) {
        val clean = value.trim().ifBlank { DEFAULT_ACCOUNT_NAME }
        prefs.edit().putString(KEY_ACCOUNT_NAME, clean).apply()
        accountName = clean
    }

    /**
     * Creates the one instance this build supports. A later repository will
     * add the multi-instance editor; until then creating again replaces the
     * selected record without touching the old files on disk.
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
            .apply()

        instance = summary
        instanceInstalled = false
        ensureInstanceDirectories(summary)
        LogStore.bootstrapOnce(logsDir(summary))
        return summary
    }

    fun updateInstance(name: String, minecraftVersion: String) {
        val current = instance ?: return
        val cleanName = name.trim().ifBlank { current.name }
        val cleanVersion = minecraftVersion.trim().ifBlank { current.minecraftVersion }
        val updated = current.copy(name = cleanName, minecraftVersion = cleanVersion)

        prefs.edit()
            .putString(KEY_INSTANCE_NAME, updated.name)
            .putString(KEY_INSTANCE_VERSION, updated.minecraftVersion)
            .apply()

        instance = updated
        instanceInstalled = false
        ensureInstanceDirectories(updated)
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

    fun ensureInstanceDirectories(summary: InstanceSummary) {
        ContentType.entries.forEach { type ->
            File(instanceDir(summary), "game/${type.directory}").mkdirs()
        }
        logsDir(summary).mkdirs()
    }

    private fun hasManifest(summary: InstanceSummary?): Boolean =
        summary != null && File(instanceDir(summary), "manifest.json").isFile

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
        const val DEFAULT_MINECRAFT_VERSION = "26.3"
        const val DEFAULT_INSTANCE_NAME = "New Instance"
        const val DEFAULT_ACCOUNT_NAME = "Player"

        private const val PREFS_NAME = "eclipse_launcher"
        private const val KEY_ACCOUNT_NAME = "account.name"
        private const val KEY_INSTANCE_ID = "instance.id"
        private const val KEY_INSTANCE_NAME = "instance.name"
        private const val KEY_INSTANCE_VERSION = "instance.version"
        private const val KEY_INSTANCE_LOADER = "instance.loader"
        private const val KEY_CURSEFORGE = "curseforge.key"
        private const val KEY_AUTO_DEPENDENCIES = "content.auto_dependencies"
    }
}

package com.eclipselauncher

import android.app.Application
import android.content.Context
import com.eclipselauncher.data.AccountStore
import com.eclipselauncher.data.InstanceStore
import com.eclipselauncher.data.Logs
import com.eclipselauncher.data.MediaStorage
import com.eclipselauncher.data.SettingsStore
import com.eclipselauncher.data.TaskStore
import com.eclipselauncher.data.VersionRepository
import java.io.File
import kotlinx.coroutines.runBlocking

class EclipseLauncherApplication : Application() {

    lateinit var container: EclipseContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = EclipseContainer(this)
        // The stores are tiny JSON files; hydrate them before the first frame
        // so the account/instance lists never flash stale data.
        runBlocking {
            container.accounts.load()
            container.instances.load()
            container.tasks.load()
        }
    }
}

/** Process-wide dependency container; replaces the reference app's Hilt graph. */
class EclipseContainer(context: Context) {
    val media: MediaStorage = MediaStorage(context)
    val accounts: AccountStore = AccountStore(File(context.filesDir, "accounts.json"))
    val instances: InstanceStore = InstanceStore(File(context.filesDir, "instances.json"), media)
    val settings: SettingsStore = SettingsStore(context)
    val versions: VersionRepository = VersionRepository(File(context.cacheDir, "meta_cache"))
    val logs: Logs = Logs(File(context.filesDir, "logs"))
    val tasks: TaskStore = TaskStore(File(context.filesDir, "tasks.json"), logs)

    /** Per-instance control maps; the editor picks files from here. */
    val controlmapsDir: File = File(context.filesDir, "controlmaps").apply { mkdirs() }
}

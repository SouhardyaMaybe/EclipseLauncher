package me.shadow.eclipselauncher.feature.version.install

import java.io.File

interface InstallTask {
    /**
     * Produce the file of this ModLoader/mod installation, running inside the caller's coroutine.
     * Failures are thrown and mapped by the caller to whatever reaction the old completion
     * callback had.
     *
     * @param customName the name of the version being installed
     * @return the downloaded installer file, or null when this task produces no file
     */
    @Throws(Exception::class)
    suspend fun run(customName: String): File?

    /**
     * Blocking bridge for callers that are not coroutines: a Java caller or a lambda already
     * running on a background executor blocks its own thread while [run] executes on it,
     * exactly like the old non-suspending `run` did.
     *
     * @param customName the name of the version being installed
     * @return the downloaded installer file, or null when this task produces no file
     */
    @Throws(Exception::class)
    fun runBlocking(customName: String): File? = kotlinx.coroutines.runBlocking {
        this@InstallTask.run(customName)
    }
}

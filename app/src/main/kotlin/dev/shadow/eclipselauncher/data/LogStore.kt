/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.data

import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Launcher-owned log files live in `.minecraft/logs/<instance>/`.
 *
 * The rule is deliberately simple and visible to the player:
 * - the first start of a process creates `latestlog.txt`;
 * - the next start renames the previous file to a timestamp and creates a new
 *   `latestlog.txt`;
 * - an activity recreation does not count as a start, which is why the guard
 *   is process-local rather than a preference.
 */
object LogStore {
    const val LATEST_NAME = "latestlog.txt"

    private val bootstrapped = mutableSetOf<String>()
    private val archiveStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)

    @Synchronized
    fun bootstrapOnce(dir: File) {
        if (!bootstrapped.add(dir.absolutePath)) return
        bootstrap(dir)
    }

    fun bootstrap(dir: File) {
        dir.mkdirs()
        val latest = File(dir, LATEST_NAME)

        if (latest.isFile) {
            val stamp = archiveStamp.format(
                Date(latest.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()),
            )
            var archive = File(dir, "$stamp.txt")
            var suffix = 2
            while (archive.exists()) {
                archive = File(dir, "${stamp}_$suffix.txt")
                suffix++
            }
            if (!latest.renameTo(archive)) {
                runCatching {
                    latest.copyTo(archive, overwrite = false)
                    latest.delete()
                }
            }
        }

        val header = buildString {
            append("Eclipse Launcher log\n")
            append("Session started: ")
            append(DateFormat.getDateTimeInstance().format(Date()))
            append('\n')
        }
        runCatching { latest.writeText(header) }
    }

    fun list(dir: File): List<File> {
        dir.mkdirs()
        return dir.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
}

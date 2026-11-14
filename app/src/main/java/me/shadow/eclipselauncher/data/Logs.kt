/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package me.shadow.eclipselauncher.data

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A tiny app-wide file logger. Lines are appended to a daily file under
 * `filesDir/logs/` as `HH:mm:ss [TAG] message`, so the logs page always has
 * real content to show. Writes are serialized on this instance's lock;
 * callers are background threads only.
 */
class Logs(filesDir: File) {
    private val dir = File(filesDir, "logs").apply { mkdirs() }
    private val lock = Any()
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    /** Appends one line to today's log file. Safe to call from any thread. */
    fun log(tag: String, message: String) {
        val now = Date()
        val line = timeFormat.format(now) + " [" + tag + "] " + message + "\n"
        synchronized(lock) {
            runCatching {
                File(dir, dayFormat.format(now) + ".log").appendText(line)
            }
        }
    }

    /** Daily log files, newest first. */
    fun files(): List<File> = synchronized(lock) {
        dir.listFiles { f -> f.isFile && f.name.endsWith(".log") }
            ?.sortedByDescending { it.name }
            .orEmpty()
    }

    /** The raw text of a log file, capped so a huge file can't wedge the UI. */
    fun read(file: File): String = synchronized(lock) {
        runCatching { file.readText().take(MAX_CHARS) }.getOrDefault("")
    }

    /** Deletes every log file. */
    fun clear() {
        synchronized(lock) {
            dir.listFiles()?.forEach { it.delete() }
        }
    }

    companion object {
        private const val MAX_CHARS = 512 * 1024
    }
}

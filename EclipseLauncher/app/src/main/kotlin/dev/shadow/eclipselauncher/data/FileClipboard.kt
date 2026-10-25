/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/**
 * An in-app file clipboard. It holds paths, not bytes, so copying a large
 * world does not duplicate it until Paste is pressed.
 */
class FileClipboard {
    enum class Mode { COPY, CUT }

    var mode: Mode? by mutableStateOf(null)
        private set

    var paths: List<String> by mutableStateOf(emptyList())
        private set

    val isEmpty: Boolean
        get() = paths.isEmpty()

    fun copy(files: Collection<File>) = set(Mode.COPY, files)

    fun cut(files: Collection<File>) = set(Mode.CUT, files)

    fun clear() {
        mode = null
        paths = emptyList()
    }

    fun files(): List<File> = paths.map(::File)

    private fun set(newMode: Mode, files: Collection<File>) {
        val real = files.map { it.absolutePath }.distinct()
        mode = if (real.isEmpty()) null else newMode
        paths = real
    }
}

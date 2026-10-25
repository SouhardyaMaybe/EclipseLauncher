/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

/**
 * Real filesystem operations for the in-app browsers. Every write is confined
 * to the app-private `.minecraft` tree; nothing here touches shared storage.
 */
object FileOps {
    private const val PREVIEW_LIMIT = 512 * 1024

    fun list(dir: File): List<File> {
        dir.mkdirs()
        return dir.listFiles()
            ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase(Locale.US) })
            ?: emptyList()
    }

    fun paste(clipboard: FileClipboard, targetDir: File): Result<Int> = runCatching {
        val mode = clipboard.mode ?: return@runCatching 0
        targetDir.mkdirs()
        var pasted = 0

        clipboard.files().forEach { source ->
            if (!source.exists()) return@forEach

            val sourcePath = source.canonicalPath
            val targetPath = targetDir.canonicalPath
            if (targetPath == sourcePath || targetPath.startsWith(sourcePath + File.separator)) {
                return@forEach
            }
            if (mode == FileClipboard.Mode.CUT &&
                source.parentFile?.canonicalPath == targetPath
            ) {
                return@forEach
            }

            val destination = uniqueDestination(targetDir, source.name)
            if (mode == FileClipboard.Mode.CUT) {
                if (!source.renameTo(destination)) {
                    source.copyRecursively(destination, overwrite = false)
                    source.deleteRecursively()
                }
            } else {
                source.copyRecursively(destination, overwrite = false)
            }
            pasted++
        }

        if (mode == FileClipboard.Mode.CUT) clipboard.clear()
        pasted
    }

    fun delete(files: Collection<File>): Result<Unit> = runCatching {
        files.forEach { file ->
            if (file.exists()) file.deleteRecursively()
        }
    }

    fun rename(file: File, newName: String): Result<File> = runCatching {
        val clean = validateName(newName)
        val destination = File(file.parentFile ?: error("File has no parent"), clean)
        require(!destination.exists()) { "A file with that name already exists" }
        require(file.renameTo(destination)) { "Could not rename the file" }
        destination
    }

    fun createDirectory(parent: File, name: String): Result<File> = runCatching {
        val clean = validateName(name)
        val destination = File(parent, clean)
        require(!destination.exists()) { "A file with that name already exists" }
        require(destination.mkdirs()) { "Could not create the folder" }
        destination
    }

    fun validateName(name: String): String {
        val clean = name.trim()
        require(clean.isNotBlank()) { "Name is empty" }
        require(!clean.contains('/') && !clean.contains('\\')) { "Name contains a slash" }
        require(clean != "." && clean != "..") { "Name is reserved" }
        return clean
    }

    fun uniqueDestination(dir: File, name: String): File {
        var candidate = File(dir, name)
        if (!candidate.exists()) return candidate

        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val extension = if (dot > 0) name.substring(dot) else ""
        var index = 2
        while (candidate.exists()) {
            candidate = File(dir, "$base ($index)$extension")
            index++
        }
        return candidate
    }

    fun share(context: Context, files: Collection<File>) {
        val real = files.filter { it.isFile }
        if (real.isEmpty()) return

        val uris = real.map { file ->
            FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        }
        val type = if (real.size == 1) mimeType(real.first()) else "*/*"
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                this.type = type
                putExtra(Intent.EXTRA_STREAM, uris.first())
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                this.type = type
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(uris))
            }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, null))
    }

    fun relativePath(root: File, file: File): String =
        runCatching {
            file.relativeTo(root).path.replace(File.separatorChar, '/')
        }.getOrDefault(file.name)

    fun formatSize(context: Context, file: File): String =
        Formatter.formatFileSize(context, file.length())

    fun isProbablyText(file: File): Boolean {
        if (!file.isFile) return false
        if (file.length() == 0L) return true
        val buffer = ByteArray(4096)
        val read = file.inputStream().use { it.read(buffer) }
        if (read <= 0) return true

        var controls = 0
        for (index in 0 until read) {
            val value = buffer[index].toInt() and 0xFF
            if (value == 0) return false
            val printable = value == 9 || value == 10 || value == 13 || value in 32..126 || value >= 128
            if (!printable) controls++
        }
        return controls * 100 / read < 10
    }

    /** Returns the text and whether the file was cut at [PREVIEW_LIMIT]. */
    fun readText(file: File): Pair<String, Boolean> {
        val length = file.length()
        val limit = minOf(length, PREVIEW_LIMIT.toLong()).toInt()
        val bytes = ByteArray(limit)
        file.inputStream().use { input ->
            var offset = 0
            while (offset < limit) {
                val read = input.read(bytes, offset, limit - offset)
                if (read <= 0) break
                offset += read
            }
        }
        return bytes.toString(Charsets.UTF_8) to (length > PREVIEW_LIMIT)
    }

    private fun mimeType(file: File): String = when (file.extension.lowercase(Locale.US)) {
        "txt", "log", "md" -> "text/plain"
        "json" -> "application/json"
        "jar" -> "application/java-archive"
        "zip" -> "application/zip"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        else -> "*/*"
    }
}

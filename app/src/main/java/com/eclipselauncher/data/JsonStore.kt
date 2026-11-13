/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package com.eclipselauncher.data

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * A tiny file-backed JSON holder. Reads and writes happen on [Dispatchers.IO];
 * writes go through a temporary file so a crash cannot truncate the store.
 * Callers serialise concurrent access with their own mutex.
 */
internal class JsonStore<T>(
    private val file: File,
    private val serializer: KSerializer<T>,
    private val default: () -> T,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun read(): T = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext default()
        runCatching {
            json.decodeFromString(serializer, file.readText())
        }.getOrElse { default() }
    }

    suspend fun write(value: T) = withContext(Dispatchers.IO) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(serializer, value))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
        Unit
    }
}

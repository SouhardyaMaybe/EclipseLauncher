/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.mojang

/** A metadata value that cannot be turned into a safe download. */
class MetadataException(message: String) : IllegalArgumentException(message)

/**
 * A Maven coordinate as it appears in a Mojang library entry.
 *
 * The parser is deliberately strict: the coordinate becomes a relative path in
 * the shared library store, so a slash, a backslash, a NUL or a `..` segment is
 * rejected before it can escape that store.
 */
data class MavenCoordinate(
    val group: String,
    val artifact: String,
    val version: String,
    val classifier: String? = null,
    val extension: String = "jar",
) {
    init {
        require(
            isSafePart(group, allowDots = true) &&
                group.split('.').none { it.isEmpty() || it == ".." },
        ) { "unsafe Maven group: $group" }
        require(isSafePart(artifact)) { "unsafe Maven artifact: $artifact" }
        require(isSafePart(version)) { "unsafe Maven version: $version" }
        require(classifier == null || isSafePart(classifier)) {
            "unsafe Maven classifier: $classifier"
        }
        require(isSafePart(extension)) { "unsafe Maven extension: $extension" }
    }

    /** The deduplication key. A classifier is part of the same module. */
    val moduleKey: String
        get() = "$group:$artifact"

    val fileName: String
        get() = buildString {
            append(artifact)
            append('-')
            append(version)
            if (classifier != null) {
                append('-')
                append(classifier)
            }
            append('.')
            append(extension)
        }

    val relativePath: String
        get() = "${group.replace('.', '/')}/$artifact/$version/$fileName"

    companion object {
        fun parse(raw: String): MavenCoordinate {
            if (raw.isBlank()) throw MetadataException("empty Maven coordinate")
            if (raw.any { it == '/' || it == '\\' || it == '\u0000' }) {
                throw MetadataException("Maven coordinate contains a path separator: $raw")
            }

            val at = raw.indexOf('@')
            val coordinatePart = if (at >= 0) raw.substring(0, at) else raw
            val extension = if (at >= 0) raw.substring(at + 1) else "jar"
            if (at >= 0 && raw.indexOf('@', at + 1) >= 0) {
                throw MetadataException("Maven coordinate has more than one extension: $raw")
            }

            val parts = coordinatePart.split(':')
            if (parts.size !in 3..4) {
                throw MetadataException("Maven coordinate must have 3 or 4 parts: $raw")
            }
            val group = parts[0]
            val artifact = parts[1]
            val version = parts[2]
            val classifier = parts.getOrNull(3)?.takeIf { it.isNotEmpty() }
            if (group.isEmpty() || artifact.isEmpty() || version.isEmpty()) {
                throw MetadataException("Maven coordinate has an empty part: $raw")
            }
            return MavenCoordinate(
                group = group,
                artifact = artifact,
                version = version,
                classifier = classifier,
                extension = extension,
            )
        }

        private fun isSafePart(value: String, allowDots: Boolean = false): Boolean {
            if (value.isEmpty()) return false
            if (value == "." || value == "..") return false
            if (value.any { it == '/' || it == '\\' || it == '\u0000' || it.isWhitespace() }) {
                return false
            }
            if (!allowDots && value.contains("..")) return false
            return value.all { it.isLetterOrDigit() || it in "._+-" }
        }
    }
}

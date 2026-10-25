/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.install

/**
 * Lowercase hexadecimal without locale or signed-byte surprises. `%02x`
 * happens to treat a [Byte] as unsigned on the JVM, but that is a formatter
 * detail, not a property this code should depend on.
 */
internal fun ByteArray.toHex(): String {
    val digits = "0123456789abcdef"
    val out = CharArray(size * 2)
    for (index in indices) {
        val value = this[index].toInt() and 0xff
        out[index * 2] = digits[value ushr 4]
        out[index * 2 + 1] = digits[value and 0x0f]
    }
    return String(out)
}

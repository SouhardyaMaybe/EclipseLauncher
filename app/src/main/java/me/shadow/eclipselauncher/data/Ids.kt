/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package me.shadow.eclipselauncher.data

import java.security.MessageDigest
import java.util.UUID

/** Random identifier for new local entities. */
fun newId(): String = UUID.randomUUID().toString()

/**
 * The offline (pseudo) UUID Minecraft assigns to a local account: the version
 * 3 UUID of the UTF-8 bytes of `OfflinePlayer:<name>`.
 */
fun offlineUuid(name: String): String {
    val digest = MessageDigest.getInstance("MD5")
        .digest("OfflinePlayer:$name".toByteArray(Charsets.UTF_8))
    digest[6] = ((digest[6].toInt() and 0x0F) or 0x30).toByte()
    digest[8] = ((digest[8].toInt() and 0x3F) or 0x80).toByte()
    val hex = digest.joinToString("") { "%02x".format(it) }
    return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-" +
        "${hex.substring(16, 20)}-${hex.substring(20, 32)}"
}

/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.core.network

import java.util.Locale

/**
 * An exact, case-insensitive host allowlist. It deliberately has no wildcard
 * syntax: a download URL is trusted only when its host is written here.
 */
class HostAllowlist(hosts: Set<String>) {
    private val allowed: Set<String>

    init {
        require(hosts.isNotEmpty()) { "a host allowlist cannot be empty" }
        val normalised = hosts.map { it.trim().lowercase(Locale.ROOT) }.toSet()
        require(normalised.none { it.isEmpty() }) { "a host allowlist cannot contain a blank host" }
        require(normalised.all { HOST.matches(it) }) {
            "a host allowlist entry is not a DNS hostname"
        }
        allowed = normalised
    }

    fun contains(host: String): Boolean = allowed.contains(host.lowercase(Locale.ROOT))

    val hosts: Set<String>
        get() = allowed

    companion object {
        private val HOST = Regex("[a-z0-9]([a-z0-9.-]*[a-z0-9])?")

        /**
         * The hosts named in docs 07 §9.1. GitHub is present for release
         * assets only; a caller that fetches metadata still has to use the
         * specific API host.
         */
        val Minecraft: HostAllowlist = HostAllowlist(
            setOf(
                "piston-meta.mojang.com",
                "piston-data.mojang.com",
                "resources.download.minecraft.net",
                "libraries.minecraft.net",
                "maven.fabricmc.net",
                "meta.fabricmc.net",
                "maven.quiltmc.org",
                "meta.quiltmc.org",
                "maven.neoforged.net",
                "api.modrinth.com",
                "cdn.modrinth.com",
                "api.github.com",
                "objects.githubusercontent.com",
                "github.com",
            ),
        )
    }
}

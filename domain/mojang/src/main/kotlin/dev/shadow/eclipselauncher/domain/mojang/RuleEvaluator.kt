/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.mojang

/**
 * The environment a Mojang rule is evaluated against.
 *
 * The launcher presents itself as Linux/arm64 because that is the ABI the
 * game's Java code and EclipseNatives are built for. It does not pretend to
 * be x86_64: the native artifacts are replaced, not emulated.
 */
data class LaunchContext(
    val osName: String = "linux",
    val osArch: String = "arm64",
    val osVersion: String? = null,
    val features: Map<String, Boolean> = emptyMap(),
)

/**
 * Evaluates the ordered rule list exactly as the version metadata specifies:
 * every matching rule replaces the previous verdict, and the last matching
 * rule wins. A list with no matching rule keeps the caller's default.
 *
 * The caller's default is not cosmetic. An empty library rule list means
 * "allowed"; a present rule list with no match means "not for this platform".
 * [VersionResolver] encodes that distinction, and [ArgumentTemplate] uses
 * `false` for argument objects.
 */
object RuleEvaluator {
    fun allows(
        rules: List<Rule>,
        context: LaunchContext,
        default: Boolean,
    ): Boolean {
        var allowed = default
        for (rule in rules) {
            if (matches(rule, context)) {
                allowed = rule.action == "allow"
            }
        }
        return allowed
    }

    fun matches(rule: Rule, context: LaunchContext): Boolean {
        val os = rule.os
        if (os != null) {
            if (os.name != null && normalizeOsName(os.name) != normalizeOsName(context.osName)) {
                return false
            }
            if (os.arch != null && normalizeArch(os.arch) != normalizeArch(context.osArch)) {
                return false
            }
            if (os.version != null) {
                val actual = context.osVersion ?: return false
                if (!Regex(os.version).matches(actual)) return false
            }
        }

        for ((feature, expected) in rule.features) {
            if ((context.features[feature] ?: false) != expected) return false
        }
        return true
    }

    private fun normalizeOsName(value: String): String = when (value.lowercase()) {
        "osx", "macos", "mac" -> "osx"
        else -> value.lowercase()
    }

    private fun normalizeArch(value: String): String = when (value.lowercase()) {
        "aarch64", "arm64", "arm64-v8a" -> "arm64"
        "amd64", "x86_64", "x86-64" -> "x86_64"
        "i386", "i486", "i586", "i686", "x86" -> "x86"
        else -> value.lowercase()
    }
}

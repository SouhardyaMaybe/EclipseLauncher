/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.mojang

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RuleEvaluatorTest {
    private val linuxArm = LaunchContext()

    @Test
    fun `an empty rule list keeps the caller default`() {
        assertThat(RuleEvaluator.allows(emptyList(), linuxArm, default = true)).isTrue()
        assertThat(RuleEvaluator.allows(emptyList(), linuxArm, default = false)).isFalse()
    }

    @Test
    fun `the last matching rule wins`() {
        val rules = listOf(
            Rule(action = "allow"),
            Rule(action = "disallow", os = OsRule(name = "linux")),
            Rule(action = "allow", os = OsRule(name = "linux")),
        )

        assertThat(RuleEvaluator.allows(rules, linuxArm, default = false)).isTrue()
    }

    @Test
    fun `a non matching rule leaves the default in place`() {
        val rules = listOf(Rule(action = "allow", os = OsRule(name = "windows")))

        assertThat(RuleEvaluator.allows(rules, linuxArm, default = false)).isFalse()
    }

    @Test
    fun `arch aliases match the Android ABI`() {
        val rules = listOf(Rule(action = "allow", os = OsRule(arch = "aarch64")))

        assertThat(RuleEvaluator.allows(rules, linuxArm, default = false)).isTrue()
        assertThat(
            RuleEvaluator.allows(
                rules,
                linuxArm.copy(osArch = "x86_64"),
                default = false,
            ),
        ).isFalse()
    }

    @Test
    fun `features must all match`() {
        val rules = listOf(
            Rule(
                action = "allow",
                features = mapOf("is_demo_user" to false, "has_custom_resolution" to true),
            ),
        )

        assertThat(
            RuleEvaluator.allows(
                rules,
                linuxArm.copy(features = mapOf("has_custom_resolution" to true)),
                default = false,
            ),
        ).isTrue()
        assertThat(
            RuleEvaluator.allows(
                rules,
                linuxArm.copy(features = mapOf("is_demo_user" to true)),
                default = false,
            ),
        ).isFalse()
    }

    @Test
    fun `os version is a regular expression`() {
        val rules = listOf(
            Rule(action = "allow", os = OsRule(version = "^13\\..*")),
        )

        assertThat(
            RuleEvaluator.allows(rules, linuxArm.copy(osVersion = "13.0.1"), default = false),
        ).isTrue()
        assertThat(
            RuleEvaluator.allows(rules, linuxArm.copy(osVersion = "12.0.1"), default = false),
        ).isFalse()
        assertThat(RuleEvaluator.allows(rules, linuxArm, default = false)).isFalse()
    }
}

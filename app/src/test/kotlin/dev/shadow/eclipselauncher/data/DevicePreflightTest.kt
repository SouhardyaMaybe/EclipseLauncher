/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.data

import com.google.common.truth.Truth.assertThat
import dev.shadow.eclipselauncher.core.common.FailureStage
import dev.shadow.eclipselauncher.core.common.UserAction
import org.junit.Test

class DevicePreflightTest {
    @Test
    fun `accepts a 64-bit arm device`() {
        assertThat(
            DevicePreflight.supportedAbi(listOf("arm64-v8a", "armeabi-v7a")),
        ).isEqualTo("arm64-v8a")
    }

    @Test
    fun `accepts the emulator ABI`() {
        assertThat(
            DevicePreflight.supportedAbi(listOf("x86_64", "x86")),
        ).isEqualTo("x86_64")
    }

    @Test
    fun `rejects a 32-bit only device with an honest failure`() {
        val abis = listOf("armeabi-v7a", "armeabi")

        assertThat(DevicePreflight.supportedAbi(abis)).isNull()

        val failure = DevicePreflight.abiFailure(abis)
        assertThat(failure.code).isEqualTo("DEVICE_ABI_UNSUPPORTED")
        assertThat(failure.stage).isEqualTo(FailureStage.INSTALL)
        assertThat(failure.retryable).isFalse()
        assertThat(failure.userAction).isEqualTo(UserAction.NONE)
        assertThat(failure.userMessage).contains("64-bit")
    }
}

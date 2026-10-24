/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class FailureTest {
    @Test
    fun `accepts a stable screaming snake case code`() {
        val failure = Failure(
            code = "RUNTIME_GLES_TOO_OLD",
            stage = FailureStage.RUNTIME,
            retryable = false,
            userMessage = "This device has GLES 3.1; this version needs 3.2.",
            userAction = UserAction.NONE,
        )

        assertThat(failure.code).isEqualTo("RUNTIME_GLES_TOO_OLD")
        assertThat(failure.userAction).isEqualTo(UserAction.NONE)
    }

    @Test
    fun `rejects a code that is not stable snake case`() {
        assertThrows(IllegalArgumentException::class.java) {
            Failure(
                code = "runtime.gles-too-old",
                stage = FailureStage.RUNTIME,
                retryable = false,
                userMessage = "This device is not supported.",
            )
        }
    }

    @Test
    fun `rejects an empty user message`() {
        assertThrows(IllegalArgumentException::class.java) {
            Failure(
                code = "NETWORK_TIMEOUT",
                stage = FailureStage.NETWORK,
                retryable = true,
                userMessage = " ",
            )
        }
    }
}

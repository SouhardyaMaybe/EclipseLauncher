/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.install

import com.google.common.truth.Truth.assertThat
import dev.shadow.eclipselauncher.core.common.AppResult
import dev.shadow.eclipselauncher.core.common.FailureStage
import dev.shadow.eclipselauncher.core.common.UserAction
import org.junit.Test

class InstallPreflightTest {
    @Test
    fun `reserves missing bytes plus scratch plus headroom`() {
        val requirement = InstallPreflight.requirement(10_000L)

        assertThat(requirement.missingBytes).isEqualTo(10_000L)
        assertThat(requirement.scratchBytes).isEqualTo(1_500L)
        assertThat(requirement.headroomBytes)
            .isEqualTo(512L * 1024L * 1024L)
        assertThat(requirement.totalBytes)
            .isEqualTo(10_000L + 1_500L + 512L * 1024L * 1024L)
    }

    @Test
    fun `an install with every object present still reserves headroom`() {
        val requirement = InstallPreflight.requirement(0L)

        assertThat(requirement.scratchBytes).isEqualTo(0L)
        assertThat(requirement.totalBytes).isEqualTo(InstallPreflight.HEADROOM_BYTES)
    }

    @Test
    fun `exactly the requirement passes`() {
        val requirement = InstallPreflight.requirement(1_000_000L)

        val result = InstallPreflight.checkSpace(
            missingBytes = 1_000_000L,
            availableBytes = requirement.totalBytes,
        )

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
    }

    @Test
    fun `one byte short is a free-space failure with the exact action`() {
        val requirement = InstallPreflight.requirement(1_000_000L)

        val result = InstallPreflight.checkSpace(
            missingBytes = 1_000_000L,
            availableBytes = requirement.totalBytes - 1L,
        )

        val failure = (result as AppResult.Error).failure
        assertThat(failure.code).isEqualTo("STORAGE_NOT_ENOUGH_SPACE")
        assertThat(failure.stage).isEqualTo(FailureStage.STORAGE)
        assertThat(failure.retryable).isFalse()
        assertThat(failure.userAction).isEqualTo(UserAction.FREE_SPACE)
        assertThat(failure.userMessage).contains("1 MiB")
    }

    @Test
    fun `a tiny shortfall is reported as at least one mebibyte`() {
        val result = InstallPreflight.checkSpace(
            missingBytes = 0L,
            availableBytes = InstallPreflight.HEADROOM_BYTES - 1L,
        )

        val failure = (result as AppResult.Error).failure
        assertThat(failure.userMessage).contains("1 MiB")
    }

    @Test
    fun `an enormous plan saturates instead of wrapping`() {
        val requirement = InstallPreflight.requirement(Long.MAX_VALUE)

        assertThat(requirement.totalBytes).isEqualTo(Long.MAX_VALUE)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative missing bytes are rejected`() {
        InstallPreflight.requirement(-1L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative available bytes are rejected`() {
        InstallPreflight.checkSpace(0L, -1L)
    }
}

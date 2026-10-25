/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.install

import dev.shadow.eclipselauncher.core.common.AppResult
import dev.shadow.eclipselauncher.core.common.Failure
import dev.shadow.eclipselauncher.core.common.FailureStage
import dev.shadow.eclipselauncher.core.common.UserAction

/**
 * The space contract from docs/07 §5.3.
 *
 * A download needs the bytes it has not fetched yet, a 15% scratch allowance,
 * and 512 MiB of headroom for the commit, logs and the next launch. The
 * headroom is deliberately charged even when every object is already in the
 * store: the instance tree and its metadata still need room, and a device
 * with no headroom tends to fail at the worst possible moment.
 */
object InstallPreflight {
    const val HEADROOM_BYTES: Long = 512L * 1024L * 1024L
    const val SCRATCH_PERCENT: Long = 15L

    private const val MIB = 1024L * 1024L

    data class SpaceRequirement(
        val missingBytes: Long,
        val scratchBytes: Long,
        val headroomBytes: Long,
        val totalBytes: Long,
    ) {
        /** Bytes still missing when [availableBytes] is on the target volume. */
        fun shortfallBytes(availableBytes: Long): Long {
            require(availableBytes >= 0L) { "available bytes must not be negative" }
            return (totalBytes - availableBytes).coerceAtLeast(0L)
        }
    }

    /**
     * Calculates the reservation without overflowing. A malicious or broken
     * plan cannot wrap the requirement into a small number and slip past the
     * check; saturation fails closed.
     */
    fun requirement(missingBytes: Long): SpaceRequirement {
        require(missingBytes >= 0L) { "missing bytes must not be negative" }
        val scratch = saturatedMultiply(missingBytes, SCRATCH_PERCENT) / 100L
        val total = saturatedAdd(saturatedAdd(missingBytes, scratch), HEADROOM_BYTES)
        return SpaceRequirement(
            missingBytes = missingBytes,
            scratchBytes = scratch,
            headroomBytes = HEADROOM_BYTES,
            totalBytes = total,
        )
    }

    fun checkSpace(missingBytes: Long, availableBytes: Long): AppResult<SpaceRequirement> {
        require(availableBytes >= 0L) { "available bytes must not be negative" }
        val requirement = requirement(missingBytes)
        if (availableBytes >= requirement.totalBytes) {
            return AppResult.Success(requirement)
        }

        val shortfall = requirement.shortfallBytes(availableBytes)
        return AppResult.Error(
            Failure(
                code = "STORAGE_NOT_ENOUGH_SPACE",
                stage = FailureStage.STORAGE,
                retryable = false,
                userMessage = "This install needs ${formatMib(shortfall)} more free space.",
                userAction = UserAction.FREE_SPACE,
                technicalDetail =
                    "needed ${requirement.totalBytes} bytes, available $availableBytes bytes",
            ),
        )
    }

    private fun formatMib(bytes: Long): String {
        val whole = bytes / MIB
        val rounded = if (bytes % MIB == 0L) whole else whole + 1L
        return "${rounded.coerceAtLeast(1L)} MiB"
    }

    private fun saturatedAdd(left: Long, right: Long): Long {
        val result = left + right
        return if (result < left) Long.MAX_VALUE else result
    }

    private fun saturatedMultiply(left: Long, right: Long): Long {
        if (left == 0L || right == 0L) return 0L
        val result = left * right
        return if (result / right != left) Long.MAX_VALUE else result
    }
}

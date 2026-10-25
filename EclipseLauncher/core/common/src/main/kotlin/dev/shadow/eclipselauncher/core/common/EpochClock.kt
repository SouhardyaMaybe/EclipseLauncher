/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.core.common

/** Injectable time, so plans and tests never read the wall clock directly. */
fun interface EpochClock {
    fun nowEpochMillis(): Long
}

object SystemEpochClock : EpochClock {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}

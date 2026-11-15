/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Cape texture validation. Vanilla capes are 64x32 and use the standard
 * 22x17 box-unwrap region at the top-left; HD capes scale that layout by an
 * integer factor. The pre-1.9 22x17 files use the same layout at scale 1.
 * Kept free of Android types so the rules run in plain JVM tests.
 */
package me.shadow.eclipselauncher.data

object CapeTextureLayout {
    const val BASE_WIDTH = 64
    const val BASE_HEIGHT = 32
    const val LEGACY_WIDTH = 22
    const val LEGACY_HEIGHT = 17

    /** Highest accepted HD factor: 1024x512, the same cap Zalith 2 uses. */
    const val MAX_SCALE = 16

    /** Returns the cape UV scale for [width]x[height], or null if it is not a cape. */
    fun scaleFor(width: Int, height: Int): Int? {
        if (width == LEGACY_WIDTH && height == LEGACY_HEIGHT) return 1
        if (width <= 0 || height <= 0) return null
        if (width % BASE_WIDTH != 0 || height % BASE_HEIGHT != 0) return null
        val scale = width / BASE_WIDTH
        if (scale < 1 || scale > MAX_SCALE) return null
        if (height / BASE_HEIGHT != scale) return null
        return scale
    }

    /** Human-readable result for the debug diagnostic. */
    fun describe(width: Int, height: Int): String {
        return when (val scale = scaleFor(width, height)) {
            null -> "$width x $height (not a cape)"
            1 -> if (width == LEGACY_WIDTH) "$width x $height legacy (1x)" else "$width x $height (1x)"
            else -> "$width x $height (${scale}x)"
        }
    }
}

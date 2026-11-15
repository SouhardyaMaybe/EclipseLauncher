/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Cape texture validation: vanilla 64x32, integer HD multiples, and the legacy
 * 22x17 layout. Anything else must be rejected without crashing.
 */
package me.shadow.eclipselauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CapeTextureTest {

    @Test
    fun acceptsVanilla64x32() {
        assertEquals(1, CapeTextureLayout.scaleFor(64, 32))
    }

    @Test
    fun acceptsIntegerHdMultiples() {
        assertEquals(2, CapeTextureLayout.scaleFor(128, 64))
        assertEquals(4, CapeTextureLayout.scaleFor(256, 128))
        assertEquals(8, CapeTextureLayout.scaleFor(512, 256))
        assertEquals(CapeTextureLayout.MAX_SCALE, CapeTextureLayout.scaleFor(1024, 512))
    }

    @Test
    fun acceptsLegacy22x17AtScaleOne() {
        assertEquals(1, CapeTextureLayout.scaleFor(22, 17))
    }

    @Test
    fun rejectsSkinSizedImage() {
        assertNull(CapeTextureLayout.scaleFor(64, 64))
    }

    @Test
    fun rejectsNonIntegerMultiples() {
        assertNull(CapeTextureLayout.scaleFor(65, 32))
        assertNull(CapeTextureLayout.scaleFor(64, 33))
        assertNull(CapeTextureLayout.scaleFor(300, 300))
        assertNull(CapeTextureLayout.scaleFor(128, 32))
        assertNull(CapeTextureLayout.scaleFor(64, 64))
    }

    @Test
    fun rejectsLegacySizeWithWrongAspectOrHeight() {
        assertNull(CapeTextureLayout.scaleFor(22, 18))
        assertNull(CapeTextureLayout.scaleFor(21, 17))
        assertNull(CapeTextureLayout.scaleFor(23, 17))
    }

    @Test
    fun rejectsOverMaxAndNonPositiveSizes() {
        assertNull(CapeTextureLayout.scaleFor(1088, 544))
        assertNull(CapeTextureLayout.scaleFor(2048, 1024))
        assertNull(CapeTextureLayout.scaleFor(0, 0))
        assertNull(CapeTextureLayout.scaleFor(-64, -32))
    }

    @Test
    fun describeNamesTheAcceptedScale() {
        assertEquals("64 x 32 (1x)", CapeTextureLayout.describe(64, 32))
        assertEquals("128 x 64 (2x)", CapeTextureLayout.describe(128, 64))
        assertEquals("22 x 17 legacy (1x)", CapeTextureLayout.describe(22, 17))
        assertEquals("64 x 64 (not a cape)", CapeTextureLayout.describe(64, 64))
    }
}

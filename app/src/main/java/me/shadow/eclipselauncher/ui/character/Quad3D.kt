/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Ported exactly from dev.obsidianreborn.android.ui.launcher.Quad3D
 */
package me.shadow.eclipselauncher.ui.character

import kotlin.jvm.internal.Intrinsics

class Quad3D(
    val v0: Vertex3D,
    val v1: Vertex3D,
    val v2: Vertex3D,
    val v3: Vertex3D,
    val isOverlay: Boolean
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Quad3D) return false
        return Intrinsics.areEqual(v0, other.v0) &&
            Intrinsics.areEqual(v1, other.v1) &&
            Intrinsics.areEqual(v2, other.v2) &&
            Intrinsics.areEqual(v3, other.v3) &&
            isOverlay == other.isOverlay
    }

    override fun hashCode(): Int {
        var result = v0.hashCode()
        result = result * 31 + v1.hashCode()
        result = result * 31 + v2.hashCode()
        result = result * 31 + v3.hashCode()
        result = result * 31 + if (isOverlay) 1 else 0
        return result
    }

    override fun toString(): String {
        return "Quad3D(v0=$v0, v1=$v1, v2=$v2, v3=$v3, isOverlay=$isOverlay)"
    }
}

/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Ported exactly from dev.obsidianreborn.android.ui.launcher.ModelPart3D
 */
package me.shadow.eclipselauncher.ui.character

import kotlin.jvm.internal.Intrinsics

class ModelPart3D(
    val name: String,
    val pivotX: Float,
    val pivotY: Float,
    val pivotZ: Float,
    val quads: List<Quad3D>,
    val children: List<ModelPart3D>,
    val isOverlay: Boolean
) {

    fun allParts(): List<ModelPart3D> {
        val result = mutableListOf<ModelPart3D>()
        result.add(this)
        for (child in children) {
            result.addAll(child.allParts())
        }
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModelPart3D) return false
        return Intrinsics.areEqual(name, other.name) &&
            java.lang.Float.floatToIntBits(pivotX) == java.lang.Float.floatToIntBits(other.pivotX) &&
            java.lang.Float.floatToIntBits(pivotY) == java.lang.Float.floatToIntBits(other.pivotY) &&
            java.lang.Float.floatToIntBits(pivotZ) == java.lang.Float.floatToIntBits(other.pivotZ) &&
            Intrinsics.areEqual(quads, other.quads) &&
            Intrinsics.areEqual(children, other.children) &&
            isOverlay == other.isOverlay
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = result * 31 + java.lang.Float.floatToIntBits(pivotX)
        result = result * 31 + java.lang.Float.floatToIntBits(pivotY)
        result = result * 31 + java.lang.Float.floatToIntBits(pivotZ)
        result = result * 31 + quads.hashCode()
        result = result * 31 + children.hashCode()
        result = result * 31 + if (isOverlay) 1 else 0
        return result
    }

    override fun toString(): String {
        return "ModelPart3D(name=$name, pivotX=$pivotX, pivotY=$pivotY, pivotZ=$pivotZ, quads=$quads, children=$children, isOverlay=$isOverlay)"
    }
}

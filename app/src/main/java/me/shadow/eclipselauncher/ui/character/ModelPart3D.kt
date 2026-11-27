/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * A single part of the hierarchical model (root → body → head/arms, legs on root).
 * Each part has a local pivot, a list of quads, and child parts.
 * Pure Kotlin — no Android types — so it stays unit-testable.
 */
package me.shadow.eclipselauncher.ui.character

/**
 * A single part of the hierarchical model (root → body → head/arms, legs on root).
 * Each part has a local pivot, a list of quads, and child parts.
 * Pure Kotlin — no Android types — so it stays unit-testable.
 */
class ModelPart3D(
    val name: String,
    val pivotX: Float,
    val pivotY: Float,
    val pivotZ: Float,
    val quads: List<Quad3D>,
    val children: List<ModelPart3D> = emptyList(),
    val isOverlay: Boolean = false
) {
    companion object {
        fun create(
            name: String,
            pivotX: Float,
            pivotY: Float,
            pivotZ: Float,
            quads: List<Quad3D>,
            children: List<ModelPart3D> = emptyList(),
            isOverlay: Boolean = false
        ): ModelPart3D {
            return ModelPart3D(name, pivotX, pivotY, pivotZ, quads, children, isOverlay)
        }
    }

    /** Returns all parts in this subtree (including this one) for flat iteration. */
    fun allParts(): List<ModelPart3D> {
        val result = mutableListOf<ModelPart3D>()
        result.add(this)
        for (child in children) {
            result.addAll(child.allParts())
        }
        return result
    }
}
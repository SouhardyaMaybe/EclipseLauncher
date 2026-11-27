/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * A single textured quad on a model part.
 * Vertices are ordered counter-clockwise when viewed from outside.
 * overlay: true for hat/jacket/sleeve/pants (renders on top of base).
 */
package me.shadow.eclipselauncher.ui.character

data class Quad3D(
    val v0: Vertex3D,
    val v1: Vertex3D,
    val v2: Vertex3D,
    val v3: Vertex3D,
    val overlay: Boolean = false
) {
    /** Returns true if this quad belongs to an overlay layer (hat, jacket, etc.). */
    fun isOverlay(): Boolean = overlay

    /** Returns the four vertices as a list for iteration. */
    fun vertices(): List<Vertex3D> = listOf(v0, v1, v2, v3)
}
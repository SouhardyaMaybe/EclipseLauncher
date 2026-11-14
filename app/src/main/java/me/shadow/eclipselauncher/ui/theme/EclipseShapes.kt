/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * The corner-radius scale. The UI stays one seamless family of rounded
 * shapes, but it is not the same radius stamped on everything: each role in
 * the hierarchy gets its own radius, so small controls read as small and
 * large panels read as large. The scale is ordered by surface area —
 *
 *   chip     10.dp   tiny inline chips and glyph wells
 *   control  12.dp   square-ish buttons (header icons, icon buttons)
 *   tile     14.dp   list rows, nametag plates, medium tiles
 *   panel    18.dp   the big structural panels (instance panel, play,
 *                    empty-state invite)
 *   dialog   24.dp   floating dialog windows
 *
 * Components take their radius from here rather than a local constant, so
 * the whole app can be retuned in one place and no screen drifts into its
 * own shapes.
 */
package me.shadow.eclipselauncher.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/** Named corner radii for the launcher's shape language. */
@Immutable
object EclipseRadius {
    /** Tiny inline chips and glyph wells. */
    val chip = 10.dp

    /** Square-ish buttons: header icons, icon buttons in rows. */
    val control = 12.dp

    /** List rows, nametag plates, medium tiles. */
    val tile = 14.dp

    /** Big structural panels: instance panel, play button, invites. */
    val panel = 18.dp

    /** Floating dialog windows. */
    val dialog = 24.dp

    /** The shape for a radius in the scale. */
    fun shape(radius: androidx.compose.ui.unit.Dp): RoundedCornerShape =
        RoundedCornerShape(radius)
}

/**
 * Material 3's shapes wired to the same scale (M3 orders its slots
 * small → extraLarge, matching chip → panel here), so stock components that
 * ask MaterialTheme for a shape land on the house radii automatically.
 */
val EclipseShapes = Shapes(
    extraSmall = RoundedCornerShape(EclipseRadius.chip),
    small = RoundedCornerShape(EclipseRadius.control),
    medium = RoundedCornerShape(EclipseRadius.tile),
    large = RoundedCornerShape(EclipseRadius.panel),
    extraLarge = RoundedCornerShape(EclipseRadius.dialog),
)

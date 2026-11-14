/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * The default skin. The primary source is the bundled steve.png
 * (res/drawable-nodpi — owner-supplied asset, no density scaling), decoded
 * once and cached; [proceduralSkin] remains as the in-code fallback if that
 * asset ever fails to decode, so the UI always has a valid 64x64 skin.
 * The layout follows the standard 64x64 format the rig expects (see
 * SkinRig's UV tables), and the hat region at (40,0)/(32,8) is what
 * SkinHeadFace draws as the overlay layer.
 */
package me.shadow.eclipselauncher.ui.character

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import me.shadow.eclipselauncher.R

/** The bundled default skin, decoded on first use. */
@Volatile
private var bundledSteve: Bitmap? = null

/**
 * The default skin for accounts without a custom one: the bundled
 * res/drawable-nodpi/steve.png, decoded once (never density-scaled) and
 * cached across every head face and 3D model that asks for it. Falls back
 * to the procedural Steve if the asset cannot be decoded. Purely
 * presentational — the returned bitmap is shared read-only.
 */
fun defaultSkin(context: Context): Bitmap {
    bundledSteve?.let { return it }
    val decoded = runCatching {
        BitmapFactory.decodeResource(
            context.applicationContext.resources,
            R.drawable.steve,
            BitmapFactory.Options().apply { inScaled = false },
        )
    }.getOrNull()
    val skin = decoded ?: proceduralSkin()
    bundledSteve = skin
    return skin
}

private const val SKIN = 0xFFC69C6D.toInt()
private const val SKIN_SHADE = 0xFFA9825A.toInt()
private const val HAIR = 0xFF37251A.toInt()
private const val SHIRT = 0xFF00AAAA.toInt()
private const val SHIRT_DARK = 0xFF00868A.toInt()
private const val PANTS = 0xFF3B4459.toInt()
private const val SHOES = 0xFF8E8E8E.toInt()
private const val SHOE_SOLE = 0xFF6E6E6E.toInt()
private const val EYE_WHITE = 0xFFFFFFFF.toInt()
private const val EYE_IRIS = 0xFF3F5C99.toInt()
private const val MOUTH = 0xFF8A6244.toInt()

/** The in-code fallback: a 64x64 bitmap painted pixel by pixel (Steve). */
fun proceduralSkin(): Bitmap {
    val w = 64
    val h = 64
    val px = IntArray(w * h) // starts fully transparent

    fun fill(u: Int, v: Int, fw: Int, fh: Int, color: Int) {
        for (y in v until v + fh) {
            for (x in u until u + fw) {
                px[y * w + x] = color
            }
        }
    }

    // ---------------------------------------------------------------- head
    fill(8, 0, 8, 8, HAIR) // top of the head
    fill(16, 0, 8, 8, SKIN_SHADE) // under the chin
    fill(0, 8, 8, 8, SKIN) // right
    fill(0, 8, 8, 3, HAIR) // hair down the right side
    fill(8, 8, 8, 8, SKIN) // front
    fill(8, 8, 8, 2, HAIR) // fringe
    fill(16, 8, 8, 8, SKIN) // left
    fill(16, 8, 8, 3, HAIR) // hair down the left side
    fill(24, 8, 8, 8, SKIN) // back
    fill(24, 8, 8, 4, HAIR) // hair across the back
    // Eyes: white on the outside, blue iris on the inside, like Steve's.
    px[12 * w + 9] = EYE_WHITE
    px[12 * w + 10] = EYE_IRIS
    px[12 * w + 13] = EYE_IRIS
    px[12 * w + 14] = EYE_WHITE
    fill(10, 14, 4, 1, MOUTH)

    // Hat overlay: hair thickness on top, a fringe lip in front, bands down
    // the sides and across the back. Skins draw this layer over the base,
    // so the head reads as real hair volume rather than a painted-on crop.
    fill(40, 0, 8, 8, HAIR)
    fill(40, 8, 8, 1, HAIR)
    fill(32, 8, 8, 3, HAIR)
    fill(48, 8, 8, 3, HAIR)
    fill(56, 8, 8, 4, HAIR)

    // ---------------------------------------------------------------- body
    fill(20, 16, 8, 4, SHIRT) // top of the tee
    fill(28, 16, 8, 4, SHIRT_DARK) // hem, seen from below
    fill(16, 20, 4, 12, SHIRT) // right side
    fill(20, 20, 8, 12, SHIRT) // front
    fill(28, 20, 4, 12, SHIRT) // left side
    fill(32, 20, 8, 12, SHIRT) // back

    // ---------------------------------------------------------------- arms
    // Short sleeves: a cyan band at the shoulder, bare skin down to the hand.
    fun arm(u: Int, v: Int) {
        fill(u + 4, v, 4, 4, SHIRT) // top of the shoulder
        fill(u + 8, v, 4, 4, SKIN) // underside of the hand
        fill(u, v + 4, 4, 12, SKIN) // outer
        fill(u + 4, v + 4, 4, 12, SKIN) // front
        fill(u + 8, v + 4, 4, 12, SKIN) // inner
        fill(u + 12, v + 4, 4, 12, SKIN) // back
        fill(u, v + 4, 4, 3, SHIRT) // sleeve band around the top
        fill(u + 4, v + 4, 4, 3, SHIRT)
        fill(u + 8, v + 4, 4, 3, SHIRT)
        fill(u + 12, v + 4, 4, 3, SHIRT)
        fill(u + 4, v + 11, 4, 1, SKIN_SHADE) // shaded wrist
    }

    arm(40, 16) // right arm
    arm(32, 48) // left arm

    // ---------------------------------------------------------------- legs
    // Trousers down to the ankle, grey shoes on the bottom rows.
    fun leg(u: Int, v: Int) {
        fill(u + 4, v, 4, 4, PANTS) // top
        fill(u + 8, v, 4, 4, SHOE_SOLE) // sole
        fill(u, v + 4, 4, 12, PANTS) // outer
        fill(u + 4, v + 4, 4, 12, PANTS) // front
        fill(u + 8, v + 4, 4, 12, PANTS) // inner
        fill(u + 12, v + 4, 4, 12, PANTS) // back
        fill(u, v + 13, 4, 3, SHOES) // shoe, all four sides
        fill(u + 4, v + 13, 4, 3, SHOES)
        fill(u + 8, v + 13, 4, 3, SHOES)
        fill(u + 12, v + 13, 4, 3, SHOES)
    }

    leg(0, 16) // right leg
    leg(16, 48) // left leg

    return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
}

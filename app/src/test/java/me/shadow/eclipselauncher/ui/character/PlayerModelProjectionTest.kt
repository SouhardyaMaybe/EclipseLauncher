/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Regression test for the CPU 3D renderer: the model asset must project into
 * the canvas around its centre, with a full-height silhouette and visible
 * front-facing quads. This catches the projection bug where raw model-space
 * coordinates were drawn at the canvas origin.
 */
package me.shadow.eclipselauncher.ui.character

import android.app.Application
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PlayerModelProjectionTest {

    @Test
    fun steveModel_projectsIntoVisibleCanvasBounds() = assertModelVisible(slim = false)

    @Test
    fun alexModel_projectsIntoVisibleCanvasBounds() = assertModelVisible(slim = true)

    private fun assertModelVisible(slim: Boolean) {
        val context = RuntimeEnvironment.getApplication()
        val model = Minecraft3DModelLoader.getPlayerModel(context, slim)
        val width = 320f
        val height = 420f
        val faces = projectModel(
            root = model,
            timeSeconds = 0f,
            animation = Player3DAnimation.IDLE,
            cx = width / 2f,
            cy = height / 2f,
            scale = (height * 0.72f) / 32f,
            bodyYaw = 0f,
            headYaw = 0f,
            headPitch = 0f,
        )

        assertTrue("model produced no visible faces", faces.size >= 18)
        assertTrue(
            "model faces are not sorted far-to-near",
            faces.zipWithNext().all { (a, b) -> a.depth >= b.depth },
        )

        // The head's front face samples the standard head-front UV rectangle
        // (8..16, 8..16). If the winding test were inverted this face would be
        // culled and only the back of the head would render.
        val headFrontVisible = faces.any { face ->
            val u = (face.u0 + face.u1 + face.u2 + face.u3) / 4f
            val v = (face.v0 + face.v1 + face.v2 + face.v3) / 4f
            kotlin.math.abs(u - 12f / 64f) < 0.01f &&
                kotlin.math.abs(v - 12f / 64f) < 0.01f
        }
        assertTrue("head front face was culled", headFrontVisible)

        val xs = faces.flatMap { listOf(it.x0, it.x1, it.x2, it.x3) }
        val ys = faces.flatMap { listOf(it.y0, it.y1, it.y2, it.y3) }
        assertTrue("projected coordinates contain NaN", xs.none { it.isNaN() } && ys.none { it.isNaN() })
        val minX = requireNotNull(xs.minOrNull())
        val maxX = requireNotNull(xs.maxOrNull())
        val minY = requireNotNull(ys.minOrNull())
        val maxY = requireNotNull(ys.maxOrNull())
        assertTrue(
            "model is not centred horizontally: $minX..$maxX",
            minX < width * 0.4f && maxX > width * 0.6f,
        )
        assertTrue(
            "model is not centred vertically: $minY..$maxY",
            minY < height * 0.3f && maxY > height * 0.7f,
        )
        assertTrue(
            "model silhouette is too short: $minY..$maxY",
            maxY - minY > height * 0.6f,
        )
    }
}

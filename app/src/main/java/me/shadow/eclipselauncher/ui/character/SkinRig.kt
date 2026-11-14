/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * The blocky player model: box geometry in skin pixels (origin at the feet,
 * +Y up, model faces +Z), the standard 64x64 skin layout (legacy 64x32
 * skins work because the left limbs reuse the right limbs' regions, which
 * is exactly what legacy textures look like), and the idle pose math. Pure
 * Kotlin — no Android types — so it stays unit-testable.
 */
package me.shadow.eclipselauncher.ui.character

import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** A rectangle in skin-texture pixels. */
data class Uv(val u: Int, val v: Int, val w: Int, val h: Int)

/** Which parts of the rig move and how. */
enum class PartKind {
    HEAD,
    BODY,
    ARM_RIGHT,
    ARM_LEFT,
    LEG_RIGHT,
    LEG_LEFT,
}

class BoxModel(
    val kind: PartKind,
    val x0: Float,
    val y0: Float,
    val z0: Float,
    val x1: Float,
    val y1: Float,
    val z1: Float,
    val inflate: Float,
    val uvFront: Uv,
    val uvBack: Uv,
    val uvRight: Uv,
    val uvLeft: Uv,
    val uvTop: Uv,
    val uvBottom: Uv,
)

/** One drawable quad after projection. Instances are pooled by [PlayerRig]. */
class RenderFace {
    /** Projected screen points: x0,y0 .. x3,y3. */
    val pts = FloatArray(8)
    var u = 0
    var v = 0
    var w = 0
    var h = 0
    var depth = 0f
}

/** All animation channels for one frame. */
data class Pose(
    val yaw: Float,
    val headYaw: Float,
    val headPitch: Float,
    val armSwing: Float,
    val breath: Float,
    /** Whole-body tilt around the hips — the skin dialog's drag pitch. */
    val bodyPitch: Float = 0f,
) {
    companion object {
        /**
         * Deterministic standing-idle choreography driven by elapsed seconds,
         * matching the idle clip of the reference animation: slow weight-shift
         * sway, steady breathing, drifting arm hang and occasional eased head
         * glances. No walk-cycle limb motion and no sit cycle.
         */
        fun at(t: Float): Pose {
            // Whole-body weight shift: a very slow left/right sway.
            val yaw = sin(t * 0.32f) * 0.35f

            // Glances: eased head targets that change every few seconds, driven
            // by a hash so the motion looks random but stays reproducible.
            val segmentLength = 5.5f
            val seg = floor(t / segmentLength)
            val within = t - seg * segmentLength
            val k = smoothstep((within / 1.3f).coerceIn(0f, 1f))
            val curYaw = hash01(seg) * 1.1f - 0.55f
            val prevYaw = hash01(seg - 1f) * 1.1f - 0.55f
            val curPitch = hash01(seg + 17f) * 0.28f - 0.14f
            val prevPitch = hash01(seg + 16f) * 0.28f - 0.14f
            val headYaw = prevYaw + (curYaw - prevYaw) * k
            val headPitch = prevPitch + (curPitch - prevPitch) * k

            return Pose(
                yaw = yaw,
                headYaw = headYaw,
                headPitch = headPitch,
                // Arms hang and drift with the sway at idle frequency — far
                // below the ~1.3Hz rhythm a walk cycle would use.
                armSwing = sin(t * 0.5f) * 0.03f,
                breath = sin(t * 1.1f) * 0.012f,
            )
        }

        private fun smoothstep(x: Float): Float {
            val c = x.coerceIn(0f, 1f)
            return c * c * (3f - 2f * c)
        }

        private fun hash01(x: Float): Float {
            val s = sin(x * 127.1f) * 43758.5453f
            return s - floor(s)
        }
    }
}

/**
 * The full rig: builds every box (base layer plus the hat/jacket/sleeve/pants
 * overlays that exist on 64x64 skins), then projects the pose into screen
 * space with an orthographic transform and painter-ordered faces.
 */
class PlayerRig {
    val faces: Array<RenderFace>
    private val boxes: List<BoxModel>

    init {
        boxes = buildBoxes()
        faces = Array(boxes.size * 6) { RenderFace() }
    }

    /**
     * Fills and returns [faces] sorted far-to-near.
     * [cx]/[cy] place model point (0, 16, 0) at screen center; [scale] is
     * pixels per skin pixel.
     */
    fun project(pose: Pose, cx: Float, cy: Float, scale: Float): Array<RenderFace> {
        beginPose(pose)
        var index = 0
        for (box in boxes) {
            writeBox(box, cx, cy, scale, index)
            index += 6
        }
        faces.sortWith(depthOrder)
        return faces
    }

    /** Projects an arbitrary model point (used for the nametag anchor). */
    fun projectPoint(
        x: Float,
        y: Float,
        z: Float,
        pose: Pose,
        cx: Float,
        cy: Float,
        scale: Float,
    ): FloatArray {
        beginPose(pose)
        transform(x, y, z, PartKind.HEAD)
        return floatArrayOf(cx + scratch[0] * scale, cy - (scratch[1] - 16f) * scale)
    }

    // --------------------------------------------------------- per-frame trig

    private var cosYaw = 1f
    private var sinYaw = 0f
    private var headYawCos = 1f
    private var headYawSin = 0f
    private var headPitchCos = 1f
    private var headPitchSin = 0f
    private var armCos = 1f
    private var armSin = 0f
    private var breathScale = 1f
    private var bodyPitchCos = 1f
    private var bodyPitchSin = 0f

    private fun beginPose(pose: Pose) {
        cosYaw = cos(pose.yaw)
        sinYaw = sin(pose.yaw)
        headYawCos = cos(pose.headYaw)
        headYawSin = sin(pose.headYaw)
        headPitchCos = cos(pose.headPitch)
        headPitchSin = sin(pose.headPitch)
        armCos = cos(pose.armSwing)
        armSin = sin(pose.armSwing)
        breathScale = 1f + pose.breath
        bodyPitchCos = cos(pose.bodyPitch)
        bodyPitchSin = sin(pose.bodyPitch)
    }

    // ------------------------------------------------------------- projection

    private fun writeBox(box: BoxModel, cx: Float, cy: Float, scale: Float, start: Int) {
        val i = box.inflate
        val x0 = box.x0 - i
        val y0 = box.y0 - i
        val z0 = box.z0 - i
        val x1 = box.x1 + i
        val y1 = box.y1 + i
        val z1 = box.z1 + i

        val kind = box.kind
        writeFace(faces[start], box.uvFront, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1, kind, cx, cy, scale)
        writeFace(faces[start + 1], box.uvBack, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0, kind, cx, cy, scale)
        // The skin layout lays the side strips out [right, front, left,
        // back] with the right one first, and the model faces +Z, so the
        // character's right cheek is the −X plane and the left cheek the
        // +X plane. The per-plane windings below were always written from
        // those planes' point of view; only the strip assignment erred.
        writeFace(faces[start + 2], box.uvLeft, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1, kind, cx, cy, scale)
        writeFace(faces[start + 3], box.uvRight, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0, kind, cx, cy, scale)
        writeFace(faces[start + 4], box.uvTop, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, kind, cx, cy, scale)
        writeFace(faces[start + 5], box.uvBottom, x1, y0, z0, x0, y0, z0, x0, y0, z1, x1, y0, z1, kind, cx, cy, scale)
    }

    private val scratch = FloatArray(3)

    private fun writeFace(
        face: RenderFace,
        uv: Uv,
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cdx: Float, cdy: Float, cdz: Float,
        dx: Float, dy: Float, dz: Float,
        kind: PartKind,
        cx: Float,
        cy: Float,
        scale: Float,
    ) {
        var depth = 0f
        val corners = floatArrayOf(ax, ay, az, bx, by, bz, cdx, cdy, cdz, dx, dy, dz)
        for (corner in 0 until 4) {
            transform(corners[corner * 3], corners[corner * 3 + 1], corners[corner * 3 + 2], kind)
            face.pts[corner * 2] = cx + scratch[0] * scale
            face.pts[corner * 2 + 1] = cy - (scratch[1] - 16f) * scale
            depth += scratch[2]
        }
        face.depth = depth / 4f
        face.u = uv.u
        face.v = uv.v
        face.w = uv.w
        face.h = uv.h
    }

    /**
     * Model-space transform: part pivots (arms hang and sway at the shoulder,
     * the head turns at the neck), then the global channels — breath scaling,
     * the whole-model yaw and the whole-body drag pitch around the hips. All
     * trig is precomputed per frame in [beginPose]; the result is a shared
     * scratch array, so read it before the next call.
     */
    private fun transform(x: Float, y: Float, z: Float, kind: PartKind) {
        var px = x
        var py = y
        var pz = z
        when (kind) {
            PartKind.HEAD -> {
                // Pitch around X through the neck (y = NECK_Y)...
                val dy = py - NECK_Y
                val ny = dy * headPitchCos - pz * headPitchSin
                val nz = dy * headPitchSin + pz * headPitchCos
                py = NECK_Y + ny
                pz = nz
                // ...then yaw around the model's vertical axis.
                val nx = px * headYawCos + pz * headYawSin
                pz = -px * headYawSin + pz * headYawCos
                px = nx
            }
            PartKind.ARM_RIGHT -> {
                val dy = py - SHOULDER_Y
                val ny = dy * armCos - pz * armSin
                val nz = dy * armSin + pz * armCos
                py = SHOULDER_Y + ny
                pz = nz
            }
            PartKind.ARM_LEFT -> {
                val dy = py - SHOULDER_Y
                val ny = dy * armCos + pz * armSin
                val nz = -dy * armSin + pz * armCos
                py = SHOULDER_Y + ny
                pz = nz
            }
            PartKind.LEG_RIGHT, PartKind.LEG_LEFT, PartKind.BODY -> Unit
        }

        // Breath: scale vertically around the chest.
        py = CHEST_Y + (py - CHEST_Y) * breathScale

        // Whole-model yaw around the vertical axis through the origin.
        val nx = px * cosYaw + pz * sinYaw
        pz = -px * sinYaw + pz * cosYaw
        px = nx

        // Whole-body pitch (the skin dialog's drag tilt) around the
        // horizontal axis at hip height, after the yaw so the model tips
        // toward wherever it currently faces — head to toes in one motion.
        val tipY = py - TIP_Y
        val tipNy = tipY * bodyPitchCos - pz * bodyPitchSin
        val tipNz = tipY * bodyPitchSin + pz * bodyPitchCos
        py = TIP_Y + tipNy
        pz = tipNz

        scratch[0] = px
        scratch[1] = py
        scratch[2] = pz
    }

    companion object {
        private const val NECK_Y = 24f
        private const val SHOULDER_Y = 23f
        private const val CHEST_Y = 12f

        /** Hip height: the fulcrum the whole-body drag tilt pivots around. */
        private const val TIP_Y = 16f

        private val depthOrder = Comparator<RenderFace> { a, b -> a.depth.compareTo(b.depth) }
    }
}

/**
 * Standard 64x64 layout boxes. Base layer first, then the overlay layer
 * (hat, jacket, sleeves, pants) as inflated duplicates — the painter's sort
 * keeps the overlay on top of its base. The inflate is per side: the hat
 * stands +0.5px out (the vanilla 9px hat box) and the other outer layers
 * +0.25px — the same totals Zalith 2's PetLib renderer adds to a part's
 * size (1.0 for the hat, 0.5 elsewhere) — so no overlay floats twice as
 * far from its base as it should.
 */
private fun buildBoxes(): List<BoxModel> {
    fun box(
        kind: PartKind,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        inflate: Float,
        front: Uv, back: Uv, right: Uv, left: Uv, top: Uv, bottom: Uv,
    ) = BoxModel(kind, x0, y0, z0, x1, y1, z1, inflate, front, back, right, left, top, bottom)

    val head = box(
        PartKind.HEAD, -4f, 24f, -4f, 4f, 32f, 4f, 0f,
        front = Uv(8, 8, 8, 8), back = Uv(24, 8, 8, 8), right = Uv(0, 8, 8, 8),
        left = Uv(16, 8, 8, 8), top = Uv(8, 0, 8, 8), bottom = Uv(16, 0, 8, 8),
    )
    val hat = box(
        PartKind.HEAD, -4f, 24f, -4f, 4f, 32f, 4f, 0.5f,
        front = Uv(40, 8, 8, 8), back = Uv(56, 8, 8, 8), right = Uv(32, 8, 8, 8),
        left = Uv(48, 8, 8, 8), top = Uv(40, 0, 8, 8), bottom = Uv(48, 0, 8, 8),
    )
    val body = box(
        PartKind.BODY, -4f, 12f, -2f, 4f, 24f, 2f, 0f,
        front = Uv(20, 20, 8, 12), back = Uv(32, 20, 8, 12), right = Uv(16, 20, 4, 12),
        left = Uv(28, 20, 4, 12), top = Uv(20, 16, 8, 4), bottom = Uv(28, 16, 8, 4),
    )
    val jacket = box(
        PartKind.BODY, -4f, 12f, -2f, 4f, 24f, 2f, 0.25f,
        front = Uv(20, 36, 8, 12), back = Uv(32, 36, 8, 12), right = Uv(16, 36, 4, 12),
        left = Uv(28, 36, 4, 12), top = Uv(20, 32, 8, 4), bottom = Uv(28, 32, 8, 4),
    )
    val armRight = box(
        PartKind.ARM_RIGHT, -8f, 12f, -2f, -4f, 24f, 2f, 0f,
        front = Uv(44, 20, 4, 12), back = Uv(52, 20, 4, 12), right = Uv(40, 20, 4, 12),
        left = Uv(48, 20, 4, 12), top = Uv(44, 16, 4, 4), bottom = Uv(48, 16, 4, 4),
    )
    val sleeveRight = box(
        PartKind.ARM_RIGHT, -8f, 12f, -2f, -4f, 24f, 2f, 0.25f,
        front = Uv(44, 36, 4, 12), back = Uv(52, 36, 4, 12), right = Uv(40, 36, 4, 12),
        left = Uv(48, 36, 4, 12), top = Uv(44, 32, 4, 4), bottom = Uv(48, 32, 4, 4),
    )
    val armLeft = box(
        PartKind.ARM_LEFT, 4f, 12f, -2f, 8f, 24f, 2f, 0f,
        front = Uv(36, 52, 4, 12), back = Uv(44, 52, 4, 12), right = Uv(32, 52, 4, 12),
        left = Uv(40, 52, 4, 12), top = Uv(36, 48, 4, 4), bottom = Uv(40, 48, 4, 4),
    )
    val sleeveLeft = box(
        PartKind.ARM_LEFT, 4f, 12f, -2f, 8f, 24f, 2f, 0.25f,
        front = Uv(52, 52, 4, 12), back = Uv(60, 52, 4, 12), right = Uv(48, 52, 4, 12),
        left = Uv(56, 52, 4, 12), top = Uv(52, 48, 4, 4), bottom = Uv(56, 48, 4, 4),
    )
    val legRight = box(
        PartKind.LEG_RIGHT, -4f, 0f, -2f, 0f, 12f, 2f, 0f,
        front = Uv(4, 20, 4, 12), back = Uv(12, 20, 4, 12), right = Uv(0, 20, 4, 12),
        left = Uv(8, 20, 4, 12), top = Uv(4, 16, 4, 4), bottom = Uv(8, 16, 4, 4),
    )
    val pantsRight = box(
        PartKind.LEG_RIGHT, -4f, 0f, -2f, 0f, 12f, 2f, 0.25f,
        front = Uv(4, 36, 4, 12), back = Uv(12, 36, 4, 12), right = Uv(0, 36, 4, 12),
        left = Uv(8, 36, 4, 12), top = Uv(4, 32, 4, 4), bottom = Uv(8, 32, 4, 4),
    )
    val legLeft = box(
        PartKind.LEG_LEFT, 0f, 0f, -2f, 4f, 12f, 2f, 0f,
        front = Uv(20, 52, 4, 12), back = Uv(28, 52, 4, 12), right = Uv(16, 52, 4, 12),
        left = Uv(24, 52, 4, 12), top = Uv(20, 48, 4, 4), bottom = Uv(24, 48, 4, 4),
    )
    val pantsLeft = box(
        PartKind.LEG_LEFT, 0f, 0f, -2f, 4f, 12f, 2f, 0.25f,
        front = Uv(4, 52, 4, 12), back = Uv(12, 52, 4, 12), right = Uv(0, 52, 4, 12),
        left = Uv(8, 52, 4, 12), top = Uv(4, 48, 4, 4), bottom = Uv(8, 48, 4, 4),
    )

    return listOf(
        head, hat,
        body, jacket,
        armRight, sleeveRight, armLeft, sleeveLeft,
        legRight, pantsRight, legLeft, pantsLeft,
    )
}

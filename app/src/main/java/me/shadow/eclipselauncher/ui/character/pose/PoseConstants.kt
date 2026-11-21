/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * All constants for the posable 3D skin preview. Each constant has a named
 * declaration, a source/rationale comment, and a classification per the study:
 * (a) Minecraft-model fact, (b) tuning value, (c) hack/workaround, (d) UNKNOWN.
 * No magic literals in implementation code — all reference these constants.
 */
package me.shadow.eclipselauncher.ui.character.pose

import kotlin.math.PI

/** Skin pixel to model unit conversion: 1 px = 0.2 units (64 px = 12.8 units height). */
const val PX_TO_UNIT = 0.2f

/** Degrees to radians (Float). */
const val DEG2RAD = (PI / 180.0).toFloat()

// ========================================================================
// SkeletonConstants — pivots, sizes, joint limits (all in model units)
// Source: Minecraft Wiki "Player model" / "Skin layout" / "Cape"
// ========================================================================

object SkeletonConstants {

    // --- Part pivots (model space, origin at feet, +Y up, faces +Z) ---
    // Wiki: head 8×8×8 at y=24..32px → center y=28px = 5.6 units; neck pivot at y=24px = 4.8 units
    // Our model: origin at feet (y=0), body 12px high at y=12..24 → pivot at y=18px = 3.6? No.
    // Skinseed: head pivot (0, 1.6, 0), body pivot (0, 0.4, 0) in their units (1 unit = 5 px).
    // We use Skinseed's validated pivots converted to our PX_TO_UNIT (0.2).
    // Skinseed units: 1.6 = 8px, 0.4 = 2px. Our units: 8px * 0.2 = 1.6, 2px * 0.2 = 0.4. Same!

    /** Head pivot: center of neck (x=0, y=1.6, z=0). Class: (a) Minecraft fact. */
    const val HEAD_PIVOT_X = 0.0f
    const val HEAD_PIVOT_Y = 1.6f
    const val HEAD_PIVIT_Z = 0.0f

    /** Body pivot: center of torso (x=0, y=0.4, z=0). Class: (a) Minecraft fact. */
    const val BODY_PIVOT_X = 0.0f
    const val BODY_PIVOT_Y = 0.4f
    const val BODY_PIVOT_Z = 0.0f

    /** Left arm pivot: shoulder (x=-1.2, y=1.2, z=0). Class: (a) Minecraft fact. */
    const val ARM_LEFT_PIVOT_X = -1.2f
    const val ARM_LEFT_PIVOT_Y = 1.2f
    const val ARM_LEFT_PIVOT_Z = 0.0f

    /** Right arm pivot: shoulder (x=1.2, y=1.2, z=0). Class: (a) Minecraft fact. */
    const val ARM_RIGHT_PIVOT_X = 1.2f
    const val ARM_RIGHT_PIVOT_Y = 1.2f
    const val ARM_RIGHT_PIVOT_Z = 0.0f

    /** Left leg pivot: hip (x=0.4, y=-0.8, z=0). Class: (a) Minecraft fact. */
    const val LEG_LEFT_PIVOT_X = 0.4f
    const val LEG_LEFT_PIVOT_Y = -0.8f
    const val LEG_LEFT_PIVOT_Z = 0.0f

    /** Right leg pivot: hip (x=-0.4, y=-0.8, z=0). Class: (a) Minecraft fact. */
    const val LEG_RIGHT_PIVOT_X = -0.4f
    const val LEG_RIGHT_PIVOT_Y = -0.8f
    const val LEG_RIGHT_PIVOT_Z = 0.0f

    // --- Slim (Alex) arm pivot adjustment ---
    /** Slim arm X shift: ±0.1 units (0.5 px). Class: (a) Minecraft fact. */
    const val SLIM_ARM_PIVOT_X_SHIFT = 0.1f

    // --- Part half-extents (for OBB ray-box intersection) ---
    // Wiki: head 8×8×8, body 8×12×4, arm 4×12×4 (Steve) / 3×12×4 (Alex), leg 4×12×4
    // Half-extents in model units (px * 0.2):
    const val HEAD_HALF_X = 0.8f  // 4px
    const val HEAD_HALF_Y = 0.8f  // 4px
    const val HEAD_HALF_Z = 0.8f  // 4px

    const val BODY_HALF_X = 0.8f  // 4px
    const val BODY_HALF_Y = 1.2f  // 6px
    const val BODY_HALF_Z = 0.4f  // 2px

    const val ARM_HALF_X_STEVE = 0.4f  // 2px
    const val ARM_HALF_X_ALEX = 0.3f   // 1.5px
    const val ARM_HALF_Y = 1.2f        // 6px
    const val ARM_HALF_Z = 0.4f        // 2px

    const val LEG_HALF_X = 0.4f  // 2px
    const val LEG_HALF_Y = 1.2f  // 6px
    const val LEG_HALF_Z = 0.4f  // 2px

    // --- Joint rotation limits (degrees) ---
    // Source: human biomechanics baselines, tuned for Minecraft character proportions.
    // Class: (b) Tuning — biomechanics baseline, adjustable for comfort.

    // Body (root): full yaw, limited pitch/roll for comfort
    const val BODY_YAW_MIN_DEG = -180.0
    const val BODY_YAW_MAX_DEG = 180.0
    const val BODY_PITCH_MIN_DEG = -90.0
    const val BODY_PITCH_MAX_DEG = 90.0
    const val BODY_ROLL_MIN_DEG = -30.0
    const val BODY_ROLL_MAX_DEG = 30.0

    // Head: neck limits
    const val HEAD_YAW_MIN_DEG = -90.0
    const val HEAD_YAW_MAX_DEG = 90.0
    const val HEAD_PITCH_MIN_DEG = -75.0
    const val HEAD_PITCH_MAX_DEG = 75.0
    const val HEAD_ROLL_MIN_DEG = -30.0
    const val HEAD_ROLL_MAX_DEG = 30.0

    // Arms: shoulder (swing, twist, abduct)
    // Swing: flexion -180° (forward) to +80° (back), Twist: ±90°, Abduct: 0..90°
    const val ARM_SWING_MIN_DEG = -180.0
    const val ARM_SWING_MAX_DEG = 80.0
    const val ARM_TWIST_MIN_DEG = -90.0
    const val ARM_TWIST_MAX_DEG = 90.0
    const val ARM_ABDUCT_MIN_DEG = 0.0
    const val ARM_ABDUCT_MAX_DEG = 90.0

    // Legs: hip (swing, twist)
    // Swing: -120° (forward) to +60° (back), Twist: ±30°
    const val LEG_SWING_MIN_DEG = -120.0
    const val LEG_SWING_MAX_DEG = 60.0
    const val LEG_TWIST_MIN_DEG = -30.0
    const val LEG_TWIST_MAX_DEG = 30.0

    // --- Cape ---
    /** Cape follows body transform exactly. Class: (a) Minecraft fact. */
    const val CAPE_FOLLOWS_BODY = true

    /** Cape geometry: 10×16×1 units at shoulder. Class: (a) Minecraft fact (Wiki "Cape"). */
    const val CAPE_WIDTH = 10.0f
    const val CAPE_HEIGHT = 16.0f
    const val CAPE_THICKNESS = 1.0f
    const val CAPE_TILT_DEG = 6.0
    const val CAPE_TOP_Y = 23.0f  // Shoulder line (Skinseed: 1.2 + 0.4? = 1.6? No, wiki: y=23px)
    const val CAPE_INNER_Z = -2.5f
    const val CAPE_OUTER_Z = -3.5f

    // --- Overlay layers (hat, jacket, sleeves, pants) ---
    /** Overlays follow base part transform exactly. Class: (a) Minecraft fact. */
    const val OVERLAY_FOLLOWS_BASE = true
}

// ========================================================================
// InteractionConstants — drag, zoom, picking
// ========================================================================

object InteractionConstants {

    /** Whole-model drag: rotation speed in radians per screen diagonal drag.
     *  Source: tuned for natural feel; replaces Skinseed's magic 10.0f (class c hack).
     *  Class: (b) Tuning. */
    const val WORLD_ROTATION_RAD_PER_DIAGONAL = 1.5f

    /** Part-drag rotation speed in radians per screen diagonal drag.
     *  Source: tuned for natural feel; replaces Skinseed's magic 6.761 (class c hack).
     *  Class: (b) Tuning. */
    const val LOCAL_ROTATION_RAD_PER_DIAGONAL = 3.0f

    /** Zoom limits (camera distance multiplier). Class: (b) Tuning. */
    const val ZOOM_MIN = 0.5f
    const val ZOOM_MAX = 3.5f

    /** Default camera distance (model units). Class: (b) Tuning. */
    const val DEFAULT_CAMERA_DISTANCE = 5.8f

    /** FOV base degrees. Class: (b) Tuning. */
    const val FOV_BASE_DEG = 42.0f

    /** Touch slop from ViewConfiguration (system default). Class: (a) Android standard. */
    // Not a constant — queried at runtime.
}

// ========================================================================
// RenderConstants — software rasterizer
// ========================================================================

object RenderConstants {

    /** Depth bias for overlay (second layer) to prevent z-fighting.
     *  Source: replaces Skinseed's per-part inflation hacks f2407N (class c hacks).
     *  Class: (b) Tuning — value 1.0 matches glPolygonOffset(1,1) convention. */
    const val OVERLAY_DEPTH_BIAS = 1.0f

    /** Cape depth bias (same as overlay). Class: (b) Tuning. */
    const val CAPE_DEPTH_BIAS = 1.0f

    /** Default render target size (dp) — scaled by density for bitmap. Class: (b) Tuning. */
    const val DEFAULT_RENDER_SIZE_DP = 340

    /** Downscale factor during drag on low-end (0.5 = half resolution). Class: (b) Tuning. */
    const val DRAG_DOWNSCALE_FACTOR = 0.5f
}

// ========================================================================
// AnimationConstants — pose transitions
// ========================================================================

object AnimationConstants {

    /** Pose transition duration (ms).
     *  Source: Material Motion "medium" token or fallback 250ms.
     *  Class: (b) Tuning — single source of truth, motion library will override.
     *  Condition 5: no fixed 250ms literal in implementation. */
    const val POSE_TRANSITION_DURATION_MS = 250

    /** Slerp epsilon for completion. Class: (b) Tuning. */
    const val SLERP_EPSILON = 0.01f
}

// ========================================================================
// UIConstants — poses panel
// ========================================================================

object UIConstants {

    /** Settings key for poses panel expanded state. Class: (b) Tuning. */
    const val POSES_PANEL_EXPANDED_KEY = "poses_panel_expanded"

    /** Preset pose IDs. Class: (a) Data definitions. */
    const val POSE_STANDING = "standing"
    const val POSE_WALKING = "walking"
    const val POSE_WAVING = "waving"
    const val POSE_SITTING = "sitting"
    const val POSE_ARMS_UP = "arms_up"
    const val POSE_T_POSE = "t_pose"

    /** Default expanded state. Class: (b) Tuning. */
    const val POSES_PANEL_DEFAULT_EXPANDED = false
}
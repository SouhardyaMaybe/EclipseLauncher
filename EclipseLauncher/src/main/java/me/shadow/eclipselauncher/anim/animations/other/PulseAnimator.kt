package me.shadow.eclipselauncher.anim.animations.other

import android.animation.Animator
import android.view.View
import me.shadow.eclipselauncher.anim.animations.BaseAnimator
import me.shadow.eclipselauncher.anim.animations.MotionCurves

/**
 * A single heartbeat-style swell: both scale axes grow by 5% and settle
 * back to their original size, peaking at the halfway point.
 */
class PulseAnimator : BaseAnimator() {
    override fun getAnimators(target: View): Array<Animator> = arrayOf(
        MotionCurves.swell(target, "scaleY", 1f, 1.05f),
        MotionCurves.swell(target, "scaleX", 1f, 1.05f)
    )
}

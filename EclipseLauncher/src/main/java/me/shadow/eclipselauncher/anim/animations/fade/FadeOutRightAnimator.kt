package me.shadow.eclipselauncher.anim.animations.fade

import android.animation.Animator
import android.view.View
import me.shadow.eclipselauncher.anim.animations.BaseAnimator
import me.shadow.eclipselauncher.anim.animations.MotionCurves

/**
 * Fades the view out while it drifts a quarter of its width to the right,
 * leaving from its laid-out position.
 */
class FadeOutRightAnimator : BaseAnimator() {
    override fun getAnimators(target: View): Array<Animator> = arrayOf(
        MotionCurves.glide(target, "alpha", 1f, 0f),
        MotionCurves.glide(target, "translationX", 0f, (target.width / 4).toFloat())
    )
}

package me.shadow.eclipselauncher.anim.animations.fade

import android.animation.Animator
import android.view.View
import me.shadow.eclipselauncher.anim.animations.BaseAnimator
import me.shadow.eclipselauncher.anim.animations.MotionCurves

/**
 * Fades the view in while it travels a quarter of its width: it starts
 * shifted to the left and glides right to its laid-out position.
 */
class FadeInLeftAnimator : BaseAnimator() {
    override fun getAnimators(target: View): Array<Animator> = arrayOf(
        MotionCurves.glide(target, "alpha", 0f, 1f),
        MotionCurves.glide(target, "translationX", (-target.width / 4).toFloat(), 0f)
    )
}

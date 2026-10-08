package me.shadow.eclipselauncher.anim.animations.fade

import android.animation.Animator
import android.view.View
import me.shadow.eclipselauncher.anim.animations.BaseAnimator
import me.shadow.eclipselauncher.anim.animations.MotionCurves

/**
 * Fades the view in while it travels a quarter of its height: it starts
 * shifted downward and glides back up to its laid-out position.
 */
class FadeInUpAnimator : BaseAnimator() {
    override fun getAnimators(target: View): Array<Animator> = arrayOf(
        MotionCurves.glide(target, "alpha", 0f, 1f),
        MotionCurves.glide(target, "translationY", (target.height / 4).toFloat(), 0f)
    )
}

package me.shadow.eclipselauncher.anim.animations.fade

import android.animation.Animator
import android.view.View
import me.shadow.eclipselauncher.anim.animations.BaseAnimator
import me.shadow.eclipselauncher.anim.animations.MotionCurves

/** Fades the view out from fully opaque to fully transparent. */
class FadeOutAnimator : BaseAnimator() {
    override fun getAnimators(target: View): Array<Animator> = arrayOf(
        MotionCurves.glide(target, "alpha", 1f, 0f)
    )
}

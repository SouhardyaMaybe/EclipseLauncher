package me.shadow.eclipselauncher.anim.animations.bounce

import android.animation.Animator
import android.view.View
import me.shadow.eclipselauncher.anim.animations.BaseAnimator
import me.shadow.eclipselauncher.anim.animations.MotionCurves

/**
 * Drops the view in from 60 px above its resting spot: it dips about 12 px
 * past the target, rebounds about 6 px above it and settles at rest while
 * fading in.
 */
class BounceInDownAnimator : BaseAnimator() {
    override fun getAnimators(target: View): Array<Animator> = arrayOf(
        MotionCurves.quickFade(target),
        MotionCurves.springEntry(target, "translationY", -60f)
    )
}

package me.shadow.eclipselauncher.anim.animations.bounce

import android.animation.Animator
import android.view.View
import me.shadow.eclipselauncher.anim.animations.BaseAnimator
import me.shadow.eclipselauncher.anim.animations.MotionCurves

/**
 * Slides the view in horizontally from 60 px on the positive-X side: it
 * swings about 12 px past the resting spot in the opposite direction,
 * rebounds about 6 px and settles at rest while fading in.
 */
class BounceInLeftAnimator : BaseAnimator() {
    override fun getAnimators(target: View): Array<Animator> = arrayOf(
        MotionCurves.quickFade(target),
        MotionCurves.springEntry(target, "translationX", 60f)
    )
}

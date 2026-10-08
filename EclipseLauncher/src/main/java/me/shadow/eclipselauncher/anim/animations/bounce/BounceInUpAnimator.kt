package me.shadow.eclipselauncher.anim.animations.bounce

import android.animation.Animator
import android.view.View
import me.shadow.eclipselauncher.anim.animations.BaseAnimator
import me.shadow.eclipselauncher.anim.animations.MotionCurves

/**
 * Springs the view up from 60 px below its resting spot: it overshoots
 * about 12 px above the target, drops about 6 px back and settles at rest
 * while fading in.
 */
class BounceInUpAnimator : BaseAnimator() {
    override fun getAnimators(target: View): Array<Animator> = arrayOf(
        MotionCurves.quickFade(target),
        MotionCurves.springEntry(target, "translationY", 60f)
    )
}

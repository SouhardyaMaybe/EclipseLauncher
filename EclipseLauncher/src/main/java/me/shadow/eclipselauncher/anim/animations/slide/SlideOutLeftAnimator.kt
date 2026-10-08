package me.shadow.eclipselauncher.anim.animations.slide

import android.animation.Animator
import android.view.View
import me.shadow.eclipselauncher.anim.animations.BaseAnimator
import me.shadow.eclipselauncher.anim.animations.MotionCurves

/**
 * Slides straight out 100 px to the left of its resting position while
 * fading out.
 */
class SlideOutLeftAnimator : BaseAnimator() {
    override fun getAnimators(target: View): Array<Animator> = arrayOf(
        MotionCurves.glide(target, "alpha", 1f, 0f),
        MotionCurves.glide(target, "translationX", 0f, -EXIT_DISTANCE)
    )

    private companion object {
        /** Travel distance in pixels. */
        const val EXIT_DISTANCE = 100f
    }
}

package me.shadow.eclipselauncher.anim.animations.slide

import android.animation.Animator
import android.view.View
import me.shadow.eclipselauncher.anim.animations.BaseAnimator
import me.shadow.eclipselauncher.anim.animations.MotionCurves

/**
 * Drops into place from 100 px above its resting position while fading in.
 */
class SlideInDownAnimator : BaseAnimator() {
    override fun getAnimators(target: View): Array<Animator> = arrayOf(
        MotionCurves.glide(target, "alpha", 0f, 1f),
        MotionCurves.glide(target, "translationY", -ENTRY_DISTANCE, 0f)
    )

    private companion object {
        /** Travel distance in pixels. */
        const val ENTRY_DISTANCE = 100f
    }
}

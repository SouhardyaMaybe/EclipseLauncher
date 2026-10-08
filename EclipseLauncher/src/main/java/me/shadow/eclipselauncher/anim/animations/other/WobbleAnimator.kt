package me.shadow.eclipselauncher.anim.animations.other

import android.animation.Animator
import android.view.View
import me.shadow.eclipselauncher.anim.animations.BaseAnimator
import me.shadow.eclipselauncher.anim.animations.MotionCurves

/**
 * Wobble in place: the view shifts side to side with swings that shrink
 * linearly from 25% down to 5% of its width while rocking from -5 degrees
 * to -1 degrees, then rests.
 */
class WobbleAnimator : BaseAnimator() {
    override fun getAnimators(target: View): Array<Animator> {
        // One unit equals one percent of the view's width.
        val percent = target.width / 100f
        return arrayOf(
            MotionCurves.swing(target, "translationX", extremes = 5, firstSign = -1f, restEarly = true) { swing ->
                MAX_SHIFT * percent * (1f - (swing - 1) / 5f)
            },
            MotionCurves.swing(target, "rotation", extremes = 5, firstSign = -1f, restEarly = false) { swing ->
                MAX_TILT - (swing - 1)
            }
        )
    }

    private companion object {
        /** Widest side-to-side swing: 25% of the view's width. */
        const val MAX_SHIFT = 25f

        /** First tilt in degrees; every swing rocks one degree less. */
        const val MAX_TILT = 5f
    }
}

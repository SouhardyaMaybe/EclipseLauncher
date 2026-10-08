package me.shadow.eclipselauncher.anim.animations.other

import android.animation.Animator
import android.view.View
import me.shadow.eclipselauncher.anim.animations.BaseAnimator
import me.shadow.eclipselauncher.anim.animations.MotionCurves
import kotlin.math.pow

/**
 * Horizontal shake: eight alternating kicks starting at 15 px, each pair of
 * swings keeping about 64% of the previous pair's reach, so the last kicks
 * are roughly 4 px wide before the view comes to rest.
 */
class ShakeAnimator : BaseAnimator() {
    override fun getAnimators(target: View): Array<Animator> = arrayOf(
        MotionCurves.swing(target, "translationX", extremes = 8, firstSign = 1f, restEarly = false) { kick ->
            START_KICK * PAIR_DECAY.pow((kick - 1) / 2)
        }
    )

    private companion object {
        /** Width of the first kick in pixels. */
        const val START_KICK = 15f

        /** Decay applied after every pair of kicks, down to 4 px at the end. */
        val PAIR_DECAY = (4f / 15f).pow(1f / 3f)
    }
}

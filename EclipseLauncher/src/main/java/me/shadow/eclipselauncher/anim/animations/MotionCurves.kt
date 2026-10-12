package me.shadow.eclipselauncher.anim.animations

import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * Motion primitives shared by the animation presets.
 *
 * A preset declares its own geometry (travel distances, swing sizes, timing
 * fractions) and hands it to the builders below. Every curve is a plain
 * function of the animator's eased progress: it is evaluated here, sampled
 * at evenly spaced progress points, and handed to [ObjectAnimator] as
 * keyframes, which then performs the property writes.
 *
 * Every animator gets an explicit ease-in-out timing curve so playback never
 * depends on platform defaults.
 */
internal object MotionCurves {

    /** Timing curve: barely moves at both ends, fastest through the middle. */
    val timing: TimeInterpolator = TimeInterpolator { input ->
        ((1.0 - cos(input.toDouble() * Math.PI)) / 2.0).toFloat()
    }

    /** Sample count for the spring entry (lands on its 40% / 80% extremes). */
    private const val SPRING_SAMPLES = 20

    /** Sample count for the pulse arch (lands on its midpoint peak). */
    private const val PULSE_SAMPLES = 8

    /**
     * Straight glide from [from] to [to] across the whole animation,
     * timed with [timing].
     */
    fun glide(target: View, property: String, from: Float, to: Float): ObjectAnimator =
        ObjectAnimator.ofFloat(target, property, from, to).also { it.interpolator = timing }

    /**
     * Fade-in used by the spring entries: alpha climbs linearly and reaches
     * full opacity one third of the way through, then holds there.
     */
    fun quickFade(target: View): ObjectAnimator =
        fromPoints(target, "alpha", 0f to 0f, 1f / 3f to 1f, 1f to 1f)

    /**
     * Springy entry that starts [start] px away from the resting position
     * (negative = above / left of rest, positive = below / right of rest),
     * swings slightly past the target, swings back the other way and lands
     * exactly at rest when progress reaches 1.
     *
     * The curve is `start * envelope(p) * cos(2.5 * PI * p)`. The envelope
     * decays from 1 to 0 and is fitted so the two overshoots sit at 20% and
     * 10% of the entry distance (12 px and 6 px for the standard 60 px
     * entry), at 40% and 80% of the eased progress.
     */
    fun springEntry(target: View, property: String, start: Float): ObjectAnimator =
        fromSamples(target, property, SPRING_SAMPLES) { progress ->
            val swing = cos(2.5 * Math.PI * progress.toDouble()).toFloat()
            start * envelope(progress) * swing
        }

    /**
     * One symmetric swell: the value rises from [base] to [peak] and settles
     * back to [base], peaking exactly at the halfway point.
     */
    fun swell(target: View, property: String, base: Float, peak: Float): ObjectAnimator =
        fromSamples(target, property, PULSE_SAMPLES) { progress ->
            base + (peak - base) * sin(Math.PI * progress.toDouble()).toFloat()
        }

    /**
     * Damped side-to-side motion with [extremes] peaks spread evenly across
     * the animation. Peaks alternate direction, beginning in [firstSign]'s
     * direction; peak k (1-based) is `sign * magnitude(k)`. When [restEarly]
     * is set the motion returns to rest one keyframe before the end and
     * holds there.
     */
    fun swing(
        target: View,
        property: String,
        extremes: Int,
        firstSign: Float,
        restEarly: Boolean,
        magnitude: (Int) -> Float
    ): ObjectAnimator {
        val denominator = extremes + 1 + (if (restEarly) 1 else 0)
        val points = ArrayList<Pair<Float, Float>>(extremes + 3)
        points += (0f to 0f)
        for (peak in 1..extremes) {
            val sign = if (peak % 2 == 1) firstSign else -firstSign
            points += ((peak.toFloat() / denominator) to (sign * magnitude(peak)))
        }
        if (restEarly) points += ((extremes + 1).toFloat() / denominator to 0f)
        points += (1f to 0f)
        return fromPoints(target, property, *points.toTypedArray())
    }

    /**
     * Decay envelope for [springEntry]: 1 at launch, 0 at rest, passing
     * through 0.2 at 40% and 0.1 at 80% of the progress (fitted cubic).
     */
    private fun envelope(progress: Float): Float =
        1f - 3.7083f * progress + 5.3125f * progress * progress - 2.6042f * progress * progress * progress

    /** Builds an animator from explicit (progress, value) keyframe points. */
    private fun fromPoints(target: View, property: String, vararg points: Pair<Float, Float>): ObjectAnimator {
        val frames = Array(points.size) { i -> Keyframe.ofFloat(points[i].first, points[i].second) }
        return ObjectAnimator.ofKeyframes(target, property, *frames).also { it.interpolator = timing }
    }

    /** Samples [curve] at [samples] evenly spaced progress points (0..1). */
    private fun fromSamples(target: View, property: String, samples: Int, curve: (Float) -> Float): ObjectAnimator {
        val frames = Array(samples + 1) { i ->
            val progress = i.toFloat() / samples
            Keyframe.ofFloat(progress, curve(progress))
        }
        return ObjectAnimator.ofKeyframes(target, property, *frames).also { it.interpolator = timing }
    }
}

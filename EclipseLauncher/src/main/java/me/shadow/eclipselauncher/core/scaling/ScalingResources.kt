package me.shadow.eclipselauncher.core.scaling

import android.content.res.Configuration
import android.content.res.Resources

import kotlin.math.floor

/**
 * Runtime companion for the scalable dimension resources bundled in
 * the values buckets (positive_sdps.xml, negative_sdps.xml and positive_ssps.xml).
 *
 * The resources expose `_Nsdp` (N = 1..600 plus `_minusNsdp` for N = 1..60) and
 * `_Nssp` (N = 1..100). The base `values/` bucket maps every name to the plain
 * dp/sp value, i.e. a scale factor of 1.0. Each port-qualified smallest-width
 * bucket `values-swXdp-port` (X = 300, 330, ... 1080) overrides every name with
 * `X / 300` times the plain value, written with two decimals. Android's
 * resource selection then picks the largest bucket whose X does not exceed the
 * device's smallest width while the device is in portrait; in landscape, or on
 * screens narrower than 300dp, the identity values apply.
 *
 * [currentScaleFactor] reimplements that same bucket logic in code so callers
 * can scale plain values (or runtime-computed geometry) exactly the way the
 * `_Nsdp` / `_Nssp` resources scale.
 */
object ScalingResources {
    /** Smallest width (dp) at which the scale factor is 1.0. */
    const val BASELINE_SMALLEST_WIDTH_DP = 300

    /** Width (dp) between two smallest-width buckets. */
    const val BUCKET_STEP_DP = 30

    /** Largest bucket shipped; screens wider than this keep this factor. */
    const val MAX_SMALLEST_WIDTH_DP = 1080

    /** Scale factor of the plain `values/` bucket. */
    const val IDENTITY_FACTOR = 1.0f

    /**
     * The factor by which the bundled `_Nsdp` / `_Nssp` resources are currently
     * scaled: `bucket / 300`, where `bucket` is the device's smallest width in
     * dp rounded down to a multiple of [BUCKET_STEP_DP] and clamped to
     * [BASELINE_SMALLEST_WIDTH_DP]..[MAX_SMALLEST_WIDTH_DP].
     *
     * Returns [IDENTITY_FACTOR] outside portrait, matching the `-port`
     * resource qualifier on the scaled buckets.
     */
    fun currentScaleFactor(resources: Resources = Resources.getSystem()): Float {
        val configuration = resources.configuration
        if (configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
            return IDENTITY_FACTOR
        }
        val smallestWidthDp = minOf(
            configuration.screenWidthDp,
            configuration.screenHeightDp
        )
        val bucket = (floor(smallestWidthDp.toFloat() / BUCKET_STEP_DP).toInt() * BUCKET_STEP_DP)
            .coerceIn(BASELINE_SMALLEST_WIDTH_DP, MAX_SMALLEST_WIDTH_DP)
        return bucket / BASELINE_SMALLEST_WIDTH_DP.toFloat()
    }
}

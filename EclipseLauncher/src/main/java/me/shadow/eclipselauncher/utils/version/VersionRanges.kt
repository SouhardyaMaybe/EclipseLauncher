package me.shadow.eclipselauncher.utils.version

/**
 * An inclusive interval of versions, ordered by [VersionNumbers.compare].
 * A null bound means that side is unbounded.
 */
class VersionRange internal constructor(
    private val minimum: String?,
    private val maximum: String?
) {
    /** True when [version] falls inside the interval (bounds included). */
    fun contains(version: String): Boolean =
        (minimum == null || VersionNumbers.compare(minimum, version) <= 0) &&
            (maximum == null || VersionNumbers.compare(version, maximum) <= 0)

    override fun toString(): String = when {
        minimum == null && maximum == null -> "all versions"
        minimum == null -> "<= $maximum"
        maximum == null -> ">= $minimum"
        else -> "[$minimum, $maximum]"
    }
}

/** Factories for the usual [VersionRange] shapes. */
object VersionRanges {

    /** Every version up to and including [maximum]. */
    fun atMost(maximum: String): VersionRange = VersionRange(null, maximum)

    /** Every version from [minimum] onward, [minimum] included. */
    fun atLeast(minimum: String): VersionRange = VersionRange(minimum, null)

    /** Versions from [minimum] to [maximum], both ends included. */
    fun between(minimum: String, maximum: String): VersionRange = VersionRange(minimum, maximum)

    /** Every version. */
    fun all(): VersionRange = VersionRange(null, null)
}

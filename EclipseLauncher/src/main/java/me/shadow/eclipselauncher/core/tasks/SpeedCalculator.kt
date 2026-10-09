package me.shadow.eclipselauncher.core.tasks

/**
 * A simple class to calculate the average Internet speed using a simple moving average.
 *
 * Single-threaded by design: every instance is fed from exactly one thread/coroutine,
 * just like before the coroutine rewrite.
 */
class SpeedCalculator @JvmOverloads constructor(averageDepth: Int = DEFAULT_AVERAGE_DEPTH) {
    private var mLastMillis = 0L
    private var mLastBytes = 0L
    private var mIndex = 0
    private val mPreviousInputs = LongArray(averageDepth)
    private var mSum = 0L

    private fun addToAverage(speed: Long): Long {
        mSum -= mPreviousInputs[mIndex]
        mSum += speed
        mPreviousInputs[mIndex] = speed
        if (++mIndex == mPreviousInputs.size) mIndex = 0
        return mSum / mPreviousInputs.size
    }

    /**
     * Update the current amount of bytes downloaded.
     * @param bytes the new amount of bytes downloaded
     * @return the current download speed in bytes per second
     */
    fun feed(bytes: Long): Long {
        val millis = System.currentTimeMillis()
        val deltaBytes = bytes - mLastBytes
        val deltaMillis = millis - mLastMillis
        mLastBytes = bytes
        mLastMillis = millis

        if (deltaMillis <= 0) return 0
        var speed = deltaBytes * 1000L / deltaMillis
        speed = minOf(speed, Long.MAX_VALUE / 2)

        return addToAverage(speed)
    }

    companion object {
        private const val DEFAULT_AVERAGE_DEPTH = 64
    }
}

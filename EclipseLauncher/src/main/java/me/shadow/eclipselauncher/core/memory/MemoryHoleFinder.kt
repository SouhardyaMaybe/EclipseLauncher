package me.shadow.eclipselauncher.core.memory

import me.shadow.eclipselauncher.core.Architecture

class MemoryHoleFinder : SelfMapsParser.Callback {
    private var mPreviousEnd = 0L
    private var mLargestHole = -1L
    private val mAddressingLimit = Architecture.getAddressSpaceLimit()

    override fun process(begin: Long, end: Long, wholeLine: String): Boolean {
        var beginAddress = begin
        if (beginAddress >= mAddressingLimit) beginAddress = mAddressingLimit
        val holeSize = beginAddress - mPreviousEnd
        if (mLargestHole < holeSize) mLargestHole = holeSize
        if (beginAddress == mAddressingLimit) return false
        mPreviousEnd = end
        return true
    }

    val largestHole: Long
        get() = mLargestHole
}

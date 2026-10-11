package me.shadow.eclipselauncher.utils.io

/** Lower-case hexadecimal encoding of byte arrays. */
object Hex {

    private val HEX_DIGITS = "0123456789abcdef".toCharArray()

    /** Encodes the data as a lower-case hex character array (two chars per byte). */
    @JvmStatic
    fun encodeHex(data: ByteArray): CharArray {
        val out = CharArray(data.size * 2)
        for (i in data.indices) {
            val value = data[i].toInt() and 0xFF
            out[i * 2] = HEX_DIGITS[value ushr 4]
            out[i * 2 + 1] = HEX_DIGITS[value and 0x0F]
        }
        return out
    }
}

package me.shadow.eclipselauncher.json

/**
 * A number that keeps the exact text found in the JSON document. Conversion to
 * a concrete numeric type only happens when the value is actually read.
 */
internal class RawNumber(private val value: String) : Number() {

    override fun toInt(): Int {
        return try {
            java.lang.Integer.parseInt(value)
        } catch (e: NumberFormatException) {
            try {
                java.lang.Long.parseLong(value).toInt()
            } catch (e2: NumberFormatException) {
                java.lang.Double.parseDouble(value).toInt()
            }
        }
    }

    override fun toByte(): Byte {
        // Same defaults as java.lang.Number: narrowing through intValue
        return toInt().toByte()
    }

    override fun toShort(): Short {
        return toInt().toShort()
    }

    override fun toLong(): Long {
        return try {
            java.lang.Long.parseLong(value)
        } catch (e: NumberFormatException) {
            java.lang.Double.parseDouble(value).toLong()
        }
    }

    override fun toFloat(): Float {
        return java.lang.Float.parseFloat(value)
    }

    override fun toDouble(): Double {
        return java.lang.Double.parseDouble(value)
    }

    override fun toString(): String {
        return value
    }
}

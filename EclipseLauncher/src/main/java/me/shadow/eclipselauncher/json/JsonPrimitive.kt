package me.shadow.eclipselauncher.json

/**
 * A JSON string, number or boolean value. Numbers parsed from a document keep
 * their raw text so no precision is lost before the value is actually consumed.
 */
class JsonPrimitive : JsonElement {

    /** Shared instances for the two boolean values. */
    companion object {
        @JvmField
        val TRUE = JsonPrimitive(true)

        @JvmField
        val FALSE = JsonPrimitive(false)
    }

    /** String, Boolean or Number. Parsed numbers are kept as [RawNumber]. */
    internal val value: Any

    constructor(bool: Boolean?) {
        if (bool == null) throw NullPointerException("bool")
        value = bool
    }

    constructor(number: Number?) {
        if (number == null) throw NullPointerException("number")
        value = number
    }

    constructor(string: String?) {
        if (string == null) throw NullPointerException("string")
        value = string
    }

    constructor(c: Char?) {
        if (c == null) throw NullPointerException("character")
        value = c.toString()
    }

    val isString: Boolean
        get() = value is String

    val isNumber: Boolean
        get() = value is Number

    val isBoolean: Boolean
        get() = value is Boolean

    override val asString: String
        get() {
            if (value is String) return value
            if (value is Number) return java.lang.String.valueOf(value)
            return if (value as Boolean) "true" else "false"
        }

    override val asNumber: Number
        get() {
            if (value is Number) return value
            return RawNumber(asString)
        }

    override val asDouble: Double
        get() {
            if (value is Number) return value.toDouble()
            return asString.toDouble()
        }

    override val asFloat: Float
        get() {
            if (value is Number) return value.toFloat()
            return asString.toFloat()
        }

    override val asLong: Long
        get() {
            if (value is Number) return value.toLong()
            return asString.toLong()
        }

    override val asInt: Int
        get() {
            if (value is Number) return value.toInt()
            return asString.toInt()
        }

    override val asShort: Short
        get() {
            if (value is Number) return value.toShort()
            return asString.toShort()
        }

    override val asByte: Byte
        get() {
            if (value is Number) return value.toByte()
            return asString.toByte()
        }

    override val asChar: Char
        get() {
            if (value is Number) return value.toInt().toChar()
            return asString[0]
        }

    override val asBoolean: Boolean
        get() {
            if (value is Boolean) return value
            return asString.toBoolean()
        }
}

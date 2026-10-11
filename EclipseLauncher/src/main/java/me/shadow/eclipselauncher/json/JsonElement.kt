package me.shadow.eclipselauncher.json

/**
 * Base class of the JSON tree model. A parsed JSON document is a tree of
 * [JsonObject], [JsonArray], [JsonPrimitive] and [JsonNull] instances.
 *
 * The API mirrors the subset of the tree model that the launcher call sites rely on,
 * so migrating away from the previous JSON library only required import changes.
 */
abstract class JsonElement {

    val isJsonObject: Boolean
        get() = this is JsonObject

    val isJsonArray: Boolean
        get() = this is JsonArray

    val isJsonPrimitive: Boolean
        get() = this is JsonPrimitive

    val isJsonNull: Boolean
        get() = this is JsonNull

    val asJsonObject: JsonObject
        get() = this as? JsonObject ?: throw IllegalStateException("Not a JSON Object: $this")

    val asJsonArray: JsonArray
        get() = this as? JsonArray ?: throw IllegalStateException("Not a JSON Array: $this")

    val asJsonPrimitive: JsonPrimitive
        get() = this as? JsonPrimitive ?: throw IllegalStateException("Not a JSON Primitive: $this")

    val asJsonNull: JsonNull
        get() = this as? JsonNull ?: throw IllegalStateException("Not a JSON Null: $this")

    open val asString: String
        get() = throw UnsupportedOperationException(javaClass.simpleName)

    open val asNumber: Number
        get() = throw UnsupportedOperationException(javaClass.simpleName)

    open val asDouble: Double
        get() = throw UnsupportedOperationException(javaClass.simpleName)

    open val asFloat: Float
        get() = throw UnsupportedOperationException(javaClass.simpleName)

    open val asLong: Long
        get() = throw UnsupportedOperationException(javaClass.simpleName)

    open val asInt: Int
        get() = throw UnsupportedOperationException(javaClass.simpleName)

    open val asShort: Short
        get() = throw UnsupportedOperationException(javaClass.simpleName)

    open val asByte: Byte
        get() = throw UnsupportedOperationException(javaClass.simpleName)

    open val asChar: Char
        get() = throw UnsupportedOperationException(javaClass.simpleName)

    open val asBoolean: Boolean
        get() = throw UnsupportedOperationException(javaClass.simpleName)

    /** Compact JSON representation of this element (no pretty printing, no HTML escaping). */
    override fun toString(): String {
        val builder = StringBuilder()
        JsonEmitter.write(this, builder, 0, false, false)
        return builder.toString()
    }
}

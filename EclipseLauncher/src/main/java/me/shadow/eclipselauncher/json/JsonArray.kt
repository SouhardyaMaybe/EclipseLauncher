package me.shadow.eclipselauncher.json

/**
 * A JSON array: an ordered list of elements. Iteration order is insertion order.
 */
class JsonArray : JsonElement(), Iterable<JsonElement> {

    private val elements = ArrayList<JsonElement>()

    fun add(element: JsonElement?) {
        elements.add(element ?: JsonNull.INSTANCE)
    }

    fun add(value: String?) {
        add(if (value == null) JsonNull.INSTANCE else JsonPrimitive(value))
    }

    fun add(value: Boolean?) {
        add(if (value == null) JsonNull.INSTANCE else JsonPrimitive(value))
    }

    fun add(value: Number?) {
        add(if (value == null) JsonNull.INSTANCE else JsonPrimitive(value))
    }

    fun add(value: Char?) {
        add(if (value == null) JsonNull.INSTANCE else JsonPrimitive(value))
    }

    fun set(index: Int, element: JsonElement?): JsonElement {
        return elements.set(index, element ?: JsonNull.INSTANCE)
    }

    fun contains(element: JsonElement): Boolean {
        return elements.contains(element)
    }

    operator fun get(i: Int): JsonElement {
        return elements[i]
    }

    fun size(): Int {
        return elements.size
    }

    fun isEmpty(): Boolean {
        return elements.isEmpty()
    }

    override fun iterator(): Iterator<JsonElement> {
        return elements.iterator()
    }

    override fun toString(): String {
        val builder = StringBuilder()
        JsonEmitter.write(this, builder, 0, false, false)
        return builder.toString()
    }
}

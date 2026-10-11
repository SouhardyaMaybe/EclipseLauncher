package me.shadow.eclipselauncher.json

import kotlin.collections.LinkedHashMap

/**
 * A JSON object: an ordered map of member names to values. Insertion order is
 * preserved so that serialized output is stable.
 */
class JsonObject : JsonElement() {

    private val members = LinkedHashMap<String, JsonElement>()

    fun add(property: String, value: JsonElement?) {
        members[property] = value ?: JsonNull.INSTANCE
    }

    fun addProperty(property: String, value: String?) {
        add(property, if (value == null) JsonNull.INSTANCE else JsonPrimitive(value))
    }

    fun addProperty(property: String, value: Number?) {
        add(property, if (value == null) JsonNull.INSTANCE else JsonPrimitive(value))
    }

    fun addProperty(property: String, value: Boolean?) {
        add(property, if (value == null) JsonNull.INSTANCE else JsonPrimitive(value))
    }

    fun addProperty(property: String, value: Char?) {
        add(property, if (value == null) JsonNull.INSTANCE else JsonPrimitive(value))
    }

    /** The member element, or null when the member is absent (like the java twin). */
    operator fun get(memberName: String): JsonElement? {
        return members[memberName]
    }

    fun has(memberName: String): Boolean {
        return members.containsKey(memberName)
    }

    fun remove(memberName: String): JsonElement? {
        return members.remove(memberName)
    }

    fun getAsJsonPrimitive(memberName: String): JsonPrimitive? {
        return members[memberName] as JsonPrimitive?
    }

    fun getAsJsonArray(memberName: String): JsonArray? {
        return members[memberName] as JsonArray?
    }

    fun getAsJsonObject(memberName: String): JsonObject? {
        return members[memberName] as JsonObject?
    }

    fun entrySet(): Set<Map.Entry<String, JsonElement>> {
        return members.entries
    }

    fun keySet(): Set<String> {
        return members.keys
    }

    fun size(): Int {
        return members.size
    }

    fun isEmpty(): Boolean {
        return members.isEmpty()
    }

    override fun toString(): String {
        val builder = StringBuilder()
        JsonEmitter.write(this, builder, 0, false, false)
        return builder.toString()
    }
}

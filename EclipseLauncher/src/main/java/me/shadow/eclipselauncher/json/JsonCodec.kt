package me.shadow.eclipselauncher.json

import java.io.File
import java.io.IOException
import java.io.Reader
import java.lang.reflect.Array as ReflectArray
import java.lang.reflect.Field
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.collections.ArrayList
import kotlin.collections.Collection
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet
import kotlin.collections.Map
import kotlin.collections.Set

import me.shadow.eclipselauncher.json.annotations.SerializedName

/**
 * Maps Java/Kotlin objects to and from JSON.
 *
 * Deserialization instantiates target classes through their no-arg constructor
 * (every class the launcher deserializes into has one), then fills in fields
 * reflectively: static and transient fields are skipped, fields
 * annotated with [SerializedName] use the annotation's name, JSON null is
 * only assigned to non-primitive fields and missing members keep the constructor
 * defaults. Serialization skips null field values (the previous library's
 * default) and honors [SerializedName] as well.
 */
class JsonCodec internal constructor(
    private val htmlSafe: Boolean,
    private val prettyPrinting: Boolean
) {

    /** A default instance: HTML escaping enabled, compact output. */
    constructor() : this(true, false)

    // ------------------------------------------------------------------ fromJson

    @Throws(JsonSyntaxException::class, JsonIOException::class)
    fun <T> fromJson(json: String?, classOfT: Class<T>?): T {
        return fromJson(json, classOfT as Type?)
    }

    @Throws(JsonSyntaxException::class, JsonIOException::class)
    fun <T> fromJson(json: String?, typeOfT: Type?): T {
        if (json == null) return silentNull()
        return fromJson(JsonParser.parseString(json), typeOfT)
    }

    @Throws(JsonSyntaxException::class, JsonIOException::class)
    fun <T> fromJson(reader: Reader?, classOfT: Class<T>?): T {
        return fromJson(reader, classOfT as Type?)
    }

    @Throws(JsonSyntaxException::class, JsonIOException::class)
    fun <T> fromJson(reader: Reader?, typeOfT: Type?): T {
        if (reader == null) return silentNull()
        return fromJson(JsonParser.parseReader(reader), typeOfT)
    }

    @Throws(JsonSyntaxException::class, JsonIOException::class)
    fun <T> fromJson(json: JsonElement?, classOfT: Class<T>?): T {
        return fromJson(json, classOfT as Type?)
    }

    @Throws(JsonSyntaxException::class, JsonIOException::class)
    fun <T> fromJson(json: JsonElement?, typeOfT: Type?): T {
        if (json == null || json.isJsonNull) return silentNull()
        try {
            @Suppress("UNCHECKED_CAST")
            return bind(typeOfT, json) as T
        } catch (e: IllegalStateException) {
            throw JsonSyntaxException(e)
        } catch (e: NumberFormatException) {
            throw JsonSyntaxException(e)
        } catch (e: JsonParseException) {
            throw e
        }
    }

    // ------------------------------------------------------------------ toJson

    fun toJson(src: Any?): String {
        return toJson(toJsonTree(src))
    }

    fun toJson(src: JsonElement?): String {
        val builder = StringBuilder()
        JsonEmitter.write(src ?: JsonNull.INSTANCE, builder, 0, prettyPrinting, htmlSafe)
        return builder.toString()
    }

    @Throws(JsonIOException::class)
    fun toJson(src: Any?, writer: Appendable) {
        toJson(toJsonTree(src), writer)
    }

    @Throws(JsonIOException::class)
    fun toJson(src: JsonElement?, writer: Appendable) {
        try {
            writer.append(toJson(src))
        } catch (e: IOException) {
            throw JsonIOException(e)
        }
    }

    fun toJsonTree(src: Any?): JsonElement {
        if (src == null) return JsonNull.INSTANCE
        return toTree(src)
    }

    // ------------------------------------------------------------------ tree -> object

    private fun bind(type: Type?, element: JsonElement): Any? {
        if (element.isJsonNull) return null
        if (type is Class<*>) {
            return bindClass(type, element)
        }
        if (type is ParameterizedType) {
            val rawType = type.rawType
            if (rawType is Class<*>) {
                return bindParameterized(rawType, type.actualTypeArguments, element)
            }
        }
        if (type is GenericArrayType) {
            return bindArray(type.genericComponentType, element)
        }
        if (type is WildcardType) {
            val upper = type.upperBounds
            if (upper.isNotEmpty()) return bind(upper[0], element)
        }
        throw JsonSyntaxException("Unsupported type: $type")
    }

    private fun bindClass(raw: Class<*>, element: JsonElement): Any? {
        // Tree model passthrough
        if (raw == JsonElement::class.java) return element
        if (raw == JsonObject::class.java) {
            if (element.isJsonObject) return element.asJsonObject
            throw JsonSyntaxException("Expected a JSON Object but was ${describe(element)}")
        }
        if (raw == JsonArray::class.java) {
            if (element.isJsonArray) return element.asJsonArray
            throw JsonSyntaxException("Expected a JSON Array but was ${describe(element)}")
        }
        if (raw == JsonPrimitive::class.java) {
            if (element.isJsonPrimitive) return element.asJsonPrimitive
            throw JsonSyntaxException("Expected a JSON Primitive but was ${describe(element)}")
        }
        if (raw == JsonNull::class.java) {
            if (element.isJsonNull) return JsonNull.INSTANCE
            throw JsonSyntaxException("Expected JSON null but was ${describe(element)}")
        }

        if (raw == Any::class.java) return toPlainObject(element)
        if (raw == File::class.java) return toFile(element)
        if (raw.isArray) return bindArray(raw.componentType, element)
        if (raw.isEnum) {
            requirePrimitive(element, raw)
            return bindEnum(raw, element.asString)
        }
        if (raw == Number::class.java) {
            requirePrimitive(element, raw)
            return element.asNumber
        }
        // Raw container types reached without type arguments, e.g. List.class
        if (Map::class.java.isAssignableFrom(raw)) return bindMap(raw, arrayOf(), element)
        if (Collection::class.java.isAssignableFrom(raw)) return bindCollection(raw, Any::class.java, element)

        if (raw == String::class.java || isBoxedScalar(raw)) {
            requirePrimitive(element, raw)
            if (raw == String::class.java) return element.asString
            if (raw == Integer.TYPE || raw == Integer::class.java) return element.asInt
            if (raw == java.lang.Long.TYPE || raw == Long::class.java) return element.asLong
            if (raw == java.lang.Double.TYPE || raw == Double::class.java) return element.asDouble
            if (raw == java.lang.Float.TYPE || raw == Float::class.java) return element.asFloat
            if (raw == java.lang.Short.TYPE || raw == Short::class.java) return element.asShort
            if (raw == java.lang.Byte.TYPE || raw == Byte::class.java) return element.asByte
            if (raw == java.lang.Boolean.TYPE || raw == Boolean::class.java) return element.asBoolean
            val text = element.asString
            if (text.length != 1) throw JsonSyntaxException("Expected a single character but was \"$text\"")
            return text[0]
        }
        // Any other class is bound as a record of its fields
        return bindPojo(raw, element)
    }

    private fun bindParameterized(raw: Class<*>, arguments: Array<Type>, element: JsonElement): Any? {
        if (Map::class.java.isAssignableFrom(raw)) {
            return bindMap(raw, arguments, element)
        }
        if (Collection::class.java.isAssignableFrom(raw)) {
            val elementType: Type = if (arguments.isNotEmpty()) arguments[0] else Any::class.java
            return bindCollection(raw, elementType, element)
        }
        // Parameterized non-container types are treated as their raw class
        return bindClassOrPojo(raw, element)
    }

    private fun bindClassOrPojo(raw: Class<*>, element: JsonElement): Any? {
        if (raw.isPrimitive || raw == String::class.java || raw == File::class.java || raw.isEnum ||
            raw == JsonElement::class.java || raw == JsonObject::class.java || raw == JsonArray::class.java ||
            raw == JsonPrimitive::class.java || raw == JsonNull::class.java || raw == Any::class.java
        ) {
            return bindClass(raw, element)
        }
        return bindPojo(raw, element)
    }

    private fun bindMap(raw: Class<*>, arguments: Array<Type>, element: JsonElement): Any? {
        if (!element.isJsonObject) throw JsonSyntaxException("Expected a JSON Object for ${raw.name} but was ${describe(element)}")
        val keyType: Type = if (arguments.isNotEmpty()) arguments[0] else String::class.java
        val valueType: Type = if (arguments.size > 1) arguments[1] else Any::class.java
        val map = newMapInstance(raw)
        for (entry in element.asJsonObject.entrySet()) {
            val key = bind(keyType, JsonPrimitive(entry.key))
            map[key] = if (entry.value.isJsonNull) null else bind(valueType, entry.value)
        }
        return map
    }

    private fun newMapInstance(raw: Class<*>): MutableMap<Any?, Any?> {
        if (raw.isInterface || Modifier.isAbstract(raw.modifiers)) {
            return LinkedHashMap()
        }
        return try {
            @Suppress("UNCHECKED_CAST")
            newInstance(raw) as MutableMap<Any?, Any?>
        } catch (e: JsonIOException) {
            LinkedHashMap()
        }
    }

    private fun bindCollection(raw: Class<*>, elementType: Type, element: JsonElement): Any? {
        if (!element.isJsonArray) throw JsonSyntaxException("Expected a JSON Array for ${raw.name} but was ${describe(element)}")
        val collection = newCollectionInstance(raw)
        for (item in element.asJsonArray) {
            collection.add(if (item.isJsonNull) null else bind(elementType, item))
        }
        return collection
    }

    private fun newCollectionInstance(raw: Class<*>): MutableCollection<Any?> {
        if (raw.isInterface || Modifier.isAbstract(raw.modifiers)) {
            if (Set::class.java.isAssignableFrom(raw)) return LinkedHashSet()
            return ArrayList()
        }
        return try {
            @Suppress("UNCHECKED_CAST")
            newInstance(raw) as MutableCollection<Any?>
        } catch (e: JsonIOException) {
            ArrayList()
        }
    }

    private fun bindArray(componentType: Type, element: JsonElement): Any? {
        if (!element.isJsonArray) throw JsonSyntaxException("Expected a JSON Array but was ${describe(element)}")
        val array = element.asJsonArray
        val componentClass = rawClassOf(componentType)
        val primitiveComponents = componentClass.isPrimitive
        val result = ReflectArray.newInstance(componentClass, array.size())
        for (i in 0 until array.size()) {
            val item = array[i]
            // Primitive arrays cannot hold null; a JSON null leaves the default value in place
            if (item.isJsonNull && primitiveComponents) continue
            ReflectArray.set(result, i, if (item.isJsonNull) null else bind(componentType, item))
        }
        return result
    }

    private fun bindPojo(raw: Class<*>, element: JsonElement): Any? {
        if (!element.isJsonObject) throw JsonSyntaxException("Expected a JSON Object for ${raw.name} but was ${describe(element)}")
        val jsonObject = element.asJsonObject
        val instance = newInstance(raw)
        var c: Class<*>? = raw
        while (c != null && c != Any::class.java) {
            for (field in c.declaredFields) {
                val modifiers = field.modifiers
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) continue
                val name = serializedFieldName(field)
                val value = jsonObject.get(name)
                if (value == null) continue
                val bound = if (value.isJsonNull) null else bind(field.genericType, value)
                if (bound == null && field.type.isPrimitive) continue
                field.isAccessible = true
                try {
                    field.set(instance, bound)
                } catch (e: IllegalAccessException) {
                    throw JsonIOException("Unable to set field ${field.name} of ${raw.name}", e)
                }
            }
            c = c.superclass
        }
        return instance
    }

    private fun bindEnum(raw: Class<*>, name: String): Any? {
        for (constant in raw.enumConstants) {
            if ((constant as Enum<*>).name == name) return constant
        }
        throw JsonSyntaxException("Unknown enum constant '$name' for ${raw.name}")
    }

    private fun toFile(element: JsonElement): File {
        if (element.isJsonPrimitive) return File(element.asString)
        if (element.isJsonObject) {
            // Files are serialized as {"path": "..."} which matches the previous library's output
            val path = element.asJsonObject.get("path")
            if (path != null && path.isJsonPrimitive) return File(path.asString)
        }
        throw JsonSyntaxException("Unable to convert $element into a File")
    }

    /** Plain-JSON view of a value with declared type Object. */
    private fun toPlainObject(element: JsonElement): Any? {
        if (element.isJsonNull) return null
        if (element.isJsonPrimitive) {
            val primitive = element.asJsonPrimitive
            if (primitive.isString) return primitive.asString
            if (primitive.isBoolean) return primitive.asBoolean
            return primitive.asDouble
        }
        if (element.isJsonArray) {
            val list = ArrayList<Any?>()
            for (item in element.asJsonArray) {
                list.add(toPlainObject(item))
            }
            return list
        }
        val map = LinkedHashMap<String, Any?>()
        for (entry in element.asJsonObject.entrySet()) {
            map[entry.key] = toPlainObject(entry.value)
        }
        return map
    }

    // ------------------------------------------------------------------ object -> tree

    private fun toTree(src: Any): JsonElement {
        if (src is JsonElement) return src
        if (src is String) return JsonPrimitive(src)
        if (src is Char) return JsonPrimitive(src.toString())
        if (src is Number) return JsonPrimitive(src)
        if (src is Boolean) return JsonPrimitive(src)
        if (src is File) {
            val jsonObject = JsonObject()
            jsonObject.addProperty("path", src.path)
            return jsonObject
        }
        if (src is Enum<*>) return JsonPrimitive(src.name)

        val raw = src.javaClass
        if (raw.isArray) {
            val array = JsonArray()
            val length = ReflectArray.getLength(src)
            for (i in 0 until length) {
                // The java twin let a null element NPE inside toTree; !! keeps that parity
                array.add(toTree(ReflectArray.get(src, i)!!))
            }
            return array
        }
        if (src is Collection<*>) {
            val array = JsonArray()
            for (item in src) {
                array.add(toTree(item!!))
            }
            return array
        }
        if (src is Map<*, *>) {
            val jsonObject = JsonObject()
            for (entry in src.entries) {
                jsonObject.add(java.lang.String.valueOf(entry.key), toTree(entry.value!!))
            }
            return jsonObject
        }
        return toTreePojo(src, raw)
    }

    private fun toTreePojo(src: Any, raw: Class<*>): JsonElement {
        val jsonObject = JsonObject()
        var c: Class<*>? = raw
        while (c != null && c != Any::class.java) {
            for (field in c.declaredFields) {
                val modifiers = field.modifiers
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) continue
                field.isAccessible = true
                val value: Any?
                try {
                    value = field.get(src)
                } catch (e: IllegalAccessException) {
                    throw JsonIOException("Unable to read field ${field.name} of ${raw.name}", e)
                }
                if (value == null) continue // Null members are omitted, matching the previous default
                jsonObject.add(serializedFieldName(field), toTree(value))
            }
            c = c.superclass
        }
        return jsonObject
    }

    // ------------------------------------------------------------------ helpers

    private companion object {

        /**
         * The java codec returned a platform-typed null for a null/JSON-null
         * document. The generic cast erases so the null flows through without a
         * runtime check, exactly like the previous platform-typed return did.
         */
        private fun <T> silentNull(): T {
            @Suppress("UNCHECKED_CAST")
            return null as T
        }

        private fun newInstance(raw: Class<*>): Any? {
            try {
                val constructor = raw.getDeclaredConstructor()
                constructor.isAccessible = true
                return constructor.newInstance()
            } catch (e: ReflectiveOperationException) {
                throw JsonIOException("Failed to instantiate ${raw.name} via its no-arg constructor", e)
            }
        }

        private fun serializedFieldName(field: Field): String {
            val annotation = field.getAnnotation(SerializedName::class.java)
            return annotation?.value ?: field.name
        }

        private fun rawClassOf(type: Type): Class<*> {
            if (type is Class<*>) return type
            if (type is ParameterizedType) {
                val raw = type.rawType
                if (raw is Class<*>) return raw
            }
            if (type is GenericArrayType) {
                val component = type.genericComponentType
                return ReflectArray.newInstance(rawClassOf(component), 0).javaClass
            }
            if (type is WildcardType) {
                val upper = type.upperBounds
                if (upper.isNotEmpty()) return rawClassOf(upper[0])
            }
            return Any::class.java
        }

        private fun describe(element: JsonElement): String {
            return when {
                element.isJsonObject -> "a JSON Object"
                element.isJsonArray -> "a JSON Array"
                element.isJsonPrimitive -> "a JSON Primitive"
                else -> "JSON null"
            }
        }

        /** True for the scalar targets that must receive a JSON primitive. */
        private fun isBoxedScalar(raw: Class<*>): Boolean {
            return raw.isPrimitive || raw == Integer::class.java || raw == Long::class.java || raw == Double::class.java ||
                raw == Float::class.java || raw == Short::class.java || raw == Byte::class.java ||
                raw == Boolean::class.java || raw == Character::class.java
        }

        private fun requirePrimitive(element: JsonElement, raw: Class<*>) {
            if (!element.isJsonPrimitive) {
                throw JsonSyntaxException("Expected a primitive value for ${raw.name} but was ${describe(element)}")
            }
        }
    }
}

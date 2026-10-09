package me.shadow.eclipselauncher.json;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps Java/Kotlin objects to and from JSON.
 *
 * <p>Deserialization instantiates target classes through their no-arg constructor
 * (every class the launcher deserializes into has one), then fills in fields
 * reflectively: {@code static} and {@code transient} fields are skipped, fields
 * annotated with {@link SerializedName} use the annotation's name, JSON null is
 * only assigned to non-primitive fields and missing members keep the constructor
 * defaults. Serialization skips null field values (the previous library's
 * default) and honors {@link SerializedName} as well.</p>
 */
public class JsonCodec {

    private final boolean htmlSafe;
    private final boolean prettyPrinting;

    /** A default instance: HTML escaping enabled, compact output. */
    public JsonCodec() {
        this(true, false);
    }

    JsonCodec(boolean htmlSafe, boolean prettyPrinting) {
        this.htmlSafe = htmlSafe;
        this.prettyPrinting = prettyPrinting;
    }

    // ------------------------------------------------------------------ fromJson

    public <T> T fromJson(String json, Class<T> classOfT) throws JsonSyntaxException, JsonIOException {
        return fromJson(json, (Type) classOfT);
    }

    public <T> T fromJson(String json, Type typeOfT) throws JsonSyntaxException, JsonIOException {
        if (json == null) return null;
        return fromJson(JsonParser.parseString(json), typeOfT);
    }

    public <T> T fromJson(Reader json, Class<T> classOfT) throws JsonSyntaxException, JsonIOException {
        return fromJson(json, (Type) classOfT);
    }

    public <T> T fromJson(Reader json, Type typeOfT) throws JsonSyntaxException, JsonIOException {
        if (json == null) return null;
        return fromJson(JsonParser.parseReader(json), typeOfT);
    }

    public <T> T fromJson(JsonElement json, Class<T> classOfT) throws JsonSyntaxException, JsonIOException {
        return fromJson(json, (Type) classOfT);
    }

    @SuppressWarnings("unchecked")
    public <T> T fromJson(JsonElement json, Type typeOfT) throws JsonSyntaxException, JsonIOException {
        if (json == null || json.isJsonNull()) return null;
        try {
            return (T) bind(typeOfT, json);
        } catch (IllegalStateException | NumberFormatException e) {
            throw new JsonSyntaxException(e);
        } catch (JsonParseException e) {
            throw e;
        }
    }

    // ------------------------------------------------------------------ toJson

    public String toJson(Object src) {
        return toJson(toJsonTree(src));
    }

    public String toJson(JsonElement src) {
        StringBuilder builder = new StringBuilder();
        JsonEmitter.write(src == null ? JsonNull.INSTANCE : src, builder, 0, prettyPrinting, htmlSafe);
        return builder.toString();
    }

    public void toJson(Object src, Appendable writer) throws JsonIOException {
        toJson(toJsonTree(src), writer);
    }

    public void toJson(JsonElement src, Appendable writer) throws JsonIOException {
        try {
            writer.append(toJson(src));
        } catch (IOException e) {
            throw new JsonIOException(e);
        }
    }

    public JsonElement toJsonTree(Object src) {
        if (src == null) return JsonNull.INSTANCE;
        return toTree(src);
    }

    // ------------------------------------------------------------------ tree -> object

    private Object bind(Type type, JsonElement element) {
        if (element.isJsonNull()) return null;
        if (type instanceof Class) {
            return bindClass((Class<?>) type, element);
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterized = (ParameterizedType) type;
            Type rawType = parameterized.getRawType();
            if (rawType instanceof Class) {
                return bindParameterized((Class<?>) rawType, parameterized.getActualTypeArguments(), element);
            }
        }
        if (type instanceof GenericArrayType) {
            return bindArray(((GenericArrayType) type).getGenericComponentType(), element);
        }
        if (type instanceof WildcardType) {
            Type[] upper = ((WildcardType) type).getUpperBounds();
            if (upper.length > 0) return bind(upper[0], element);
        }
        throw new JsonSyntaxException("Unsupported type: " + type);
    }

    private Object bindClass(Class<?> raw, JsonElement element) {
        // Tree model passthrough
        if (raw == JsonElement.class) return element;
        if (raw == JsonObject.class) {
            if (element.isJsonObject()) return element.getAsJsonObject();
            throw new JsonSyntaxException("Expected a JSON Object but was " + describe(element));
        }
        if (raw == JsonArray.class) {
            if (element.isJsonArray()) return element.getAsJsonArray();
            throw new JsonSyntaxException("Expected a JSON Array but was " + describe(element));
        }
        if (raw == JsonPrimitive.class) {
            if (element.isJsonPrimitive()) return element.getAsJsonPrimitive();
            throw new JsonSyntaxException("Expected a JSON Primitive but was " + describe(element));
        }
        if (raw == JsonNull.class) {
            if (element.isJsonNull()) return JsonNull.INSTANCE;
            throw new JsonSyntaxException("Expected JSON null but was " + describe(element));
        }

        if (raw == Object.class) return toPlainObject(element);
        if (raw == File.class) return toFile(element);
        if (raw.isArray()) return bindArray(raw.getComponentType(), element);
        if (raw.isEnum()) {
            requirePrimitive(element, raw);
            return bindEnum(raw, element.getAsString());
        }
        if (raw == Number.class) {
            requirePrimitive(element, raw);
            return element.getAsNumber();
        }
        // Raw container types reached without type arguments, e.g. List.class
        if (Map.class.isAssignableFrom(raw)) return bindMap(raw, new Type[0], element);
        if (Collection.class.isAssignableFrom(raw)) return bindCollection(raw, Object.class, element);

        if (raw == String.class || isBoxedScalar(raw)) {
            requirePrimitive(element, raw);
            if (raw == String.class) return element.getAsString();
            if (raw == int.class || raw == Integer.class) return element.getAsInt();
            if (raw == long.class || raw == Long.class) return element.getAsLong();
            if (raw == double.class || raw == Double.class) return element.getAsDouble();
            if (raw == float.class || raw == Float.class) return element.getAsFloat();
            if (raw == short.class || raw == Short.class) return element.getAsShort();
            if (raw == byte.class || raw == Byte.class) return element.getAsByte();
            if (raw == boolean.class || raw == Boolean.class) return element.getAsBoolean();
            String text = element.getAsString();
            if (text.length() != 1) throw new JsonSyntaxException("Expected a single character but was \"" + text + "\"");
            return text.charAt(0);
        }
        // Any other class is bound as a record of its fields
        return bindPojo(raw, element);
    }

    /** True for the scalar targets that must receive a JSON primitive. */
    private static boolean isBoxedScalar(Class<?> raw) {
        return raw.isPrimitive() || raw == Integer.class || raw == Long.class || raw == Double.class
                || raw == Float.class || raw == Short.class || raw == Byte.class || raw == Boolean.class
                || raw == Character.class;
    }

    private static void requirePrimitive(JsonElement element, Class<?> raw) {
        if (!element.isJsonPrimitive()) {
            throw new JsonSyntaxException("Expected a primitive value for " + raw.getName() + " but was " + describe(element));
        }
    }

    private Object bindParameterized(Class<?> raw, Type[] arguments, JsonElement element) {
        if (Map.class.isAssignableFrom(raw)) {
            return bindMap(raw, arguments, element);
        }
        if (Collection.class.isAssignableFrom(raw)) {
            Type elementType = arguments.length > 0 ? arguments[0] : Object.class;
            return bindCollection(raw, elementType, element);
        }
        // Parameterized non-container types are treated as their raw class
        return bindClassOrPojo(raw, element);
    }

    private Object bindClassOrPojo(Class<?> raw, JsonElement element) {
        if (raw.isPrimitive() || raw == String.class || raw == File.class || raw.isEnum()
                || raw == JsonElement.class || raw == JsonObject.class || raw == JsonArray.class
                || raw == JsonPrimitive.class || raw == JsonNull.class || raw == Object.class) {
            return bindClass(raw, element);
        }
        return bindPojo(raw, element);
    }

    private Object bindMap(Class<?> raw, Type[] arguments, JsonElement element) {
        if (!element.isJsonObject()) throw new JsonSyntaxException("Expected a JSON Object for " + raw.getName() + " but was " + describe(element));
        Type keyType = arguments.length > 0 ? arguments[0] : String.class;
        Type valueType = arguments.length > 1 ? arguments[1] : Object.class;
        Map<Object, Object> map = newMapInstance(raw);
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            Object key = bind(keyType, new JsonPrimitive(entry.getKey()));
            map.put(key, entry.getValue().isJsonNull() ? null : bind(valueType, entry.getValue()));
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Object> newMapInstance(Class<?> raw) {
        if (raw.isInterface() || Modifier.isAbstract(raw.getModifiers())) {
            return new LinkedHashMap<>();
        }
        try {
            return (Map<Object, Object>) newInstance(raw);
        } catch (JsonIOException e) {
            return new LinkedHashMap<>();
        }
    }

    private Object bindCollection(Class<?> raw, Type elementType, JsonElement element) {
        if (!element.isJsonArray()) throw new JsonSyntaxException("Expected a JSON Array for " + raw.getName() + " but was " + describe(element));
        Collection<Object> collection = newCollectionInstance(raw);
        for (JsonElement item : element.getAsJsonArray()) {
            collection.add(item.isJsonNull() ? null : bind(elementType, item));
        }
        return collection;
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> newCollectionInstance(Class<?> raw) {
        if (raw.isInterface() || Modifier.isAbstract(raw.getModifiers())) {
            if (Set.class.isAssignableFrom(raw)) return new LinkedHashSet<>();
            return new ArrayList<>();
        }
        try {
            return (Collection<Object>) newInstance(raw);
        } catch (JsonIOException e) {
            return new ArrayList<>();
        }
    }

    private Object bindArray(Type componentType, JsonElement element) {
        if (!element.isJsonArray()) throw new JsonSyntaxException("Expected a JSON Array but was " + describe(element));
        JsonArray array = element.getAsJsonArray();
        Class<?> componentClass = rawClassOf(componentType);
        boolean primitiveComponents = componentClass.isPrimitive();
        Object result = Array.newInstance(componentClass, array.size());
        for (int i = 0; i < array.size(); i++) {
            JsonElement item = array.get(i);
            // Primitive arrays cannot hold null; a JSON null leaves the default value in place
            if (item.isJsonNull() && primitiveComponents) continue;
            Array.set(result, i, item.isJsonNull() ? null : bind(componentType, item));
        }
        return result;
    }

    private Object bindPojo(Class<?> raw, JsonElement element) {
        if (!element.isJsonObject()) throw new JsonSyntaxException("Expected a JSON Object for " + raw.getName() + " but was " + describe(element));
        JsonObject object = element.getAsJsonObject();
        Object instance = newInstance(raw);
        for (Class<?> c = raw; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) continue;
                String name = serializedFieldName(field);
                JsonElement value = object.get(name);
                if (value == null) continue;
                Object bound = value.isJsonNull() ? null : bind(field.getGenericType(), value);
                if (bound == null && field.getType().isPrimitive()) continue;
                field.setAccessible(true);
                try {
                    field.set(instance, bound);
                } catch (IllegalAccessException e) {
                    throw new JsonIOException("Unable to set field " + field.getName() + " of " + raw.getName(), e);
                }
            }
        }
        return instance;
    }

    private Object bindEnum(Class<?> raw, String name) {
        for (Object constant : raw.getEnumConstants()) {
            if (((Enum<?>) constant).name().equals(name)) return constant;
        }
        throw new JsonSyntaxException("Unknown enum constant '" + name + "' for " + raw.getName());
    }

    private File toFile(JsonElement element) {
        if (element.isJsonPrimitive()) return new File(element.getAsString());
        if (element.isJsonObject()) {
            // Files are serialized as {"path": "..."} which matches the previous library's output
            JsonElement path = element.getAsJsonObject().get("path");
            if (path != null && path.isJsonPrimitive()) return new File(path.getAsString());
        }
        throw new JsonSyntaxException("Unable to convert " + element + " into a File");
    }

    /** Plain-JSON view of a value with declared type Object. */
    private Object toPlainObject(JsonElement element) {
        if (element.isJsonNull()) return null;
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) return primitive.getAsString();
            if (primitive.isBoolean()) return primitive.getAsBoolean();
            return primitive.getAsDouble();
        }
        if (element.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonElement item : element.getAsJsonArray()) {
                list.add(toPlainObject(item));
            }
            return list;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            map.put(entry.getKey(), toPlainObject(entry.getValue()));
        }
        return map;
    }

    private static Object newInstance(Class<?> raw) {
        try {
            Constructor<?> constructor = raw.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new JsonIOException("Failed to instantiate " + raw.getName() + " via its no-arg constructor", e);
        }
    }

    // ------------------------------------------------------------------ object -> tree

    private JsonElement toTree(Object src) {
        if (src instanceof JsonElement) return (JsonElement) src;
        if (src instanceof String) return new JsonPrimitive((String) src);
        if (src instanceof Character) return new JsonPrimitive(src.toString());
        if (src instanceof Number) return new JsonPrimitive((Number) src);
        if (src instanceof Boolean) return new JsonPrimitive((Boolean) src);
        if (src instanceof File) {
            JsonObject object = new JsonObject();
            object.addProperty("path", ((File) src).getPath());
            return object;
        }
        if (src instanceof Enum) return new JsonPrimitive(((Enum<?>) src).name());

        Class<?> raw = src.getClass();
        if (raw.isArray()) {
            JsonArray array = new JsonArray();
            int length = Array.getLength(src);
            for (int i = 0; i < length; i++) {
                array.add(toTree(Array.get(src, i)));
            }
            return array;
        }
        if (src instanceof Collection) {
            JsonArray array = new JsonArray();
            for (Object item : (Collection<?>) src) {
                array.add(toTree(item));
            }
            return array;
        }
        if (src instanceof Map) {
            JsonObject object = new JsonObject();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) src).entrySet()) {
                object.add(String.valueOf(entry.getKey()), toTree(entry.getValue()));
            }
            return object;
        }
        return toTreePojo(src, raw);
    }

    private JsonElement toTreePojo(Object src, Class<?> raw) {
        JsonObject object = new JsonObject();
        for (Class<?> c = raw; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) continue;
                field.setAccessible(true);
                Object value;
                try {
                    value = field.get(src);
                } catch (IllegalAccessException e) {
                    throw new JsonIOException("Unable to read field " + field.getName() + " of " + raw.getName(), e);
                }
                if (value == null) continue; // Null members are omitted, matching the previous default
                object.add(serializedFieldName(field), toTree(value));
            }
        }
        return object;
    }

    // ------------------------------------------------------------------ helpers

    private static String serializedFieldName(Field field) {
        SerializedName annotation = field.getAnnotation(SerializedName.class);
        return annotation != null ? annotation.value() : field.getName();
    }

    private static Class<?> rawClassOf(Type type) {
        if (type instanceof Class) return (Class<?>) type;
        if (type instanceof ParameterizedType) {
            Type raw = ((ParameterizedType) type).getRawType();
            if (raw instanceof Class) return (Class<?>) raw;
        }
        if (type instanceof GenericArrayType) {
            Type component = ((GenericArrayType) type).getGenericComponentType();
            return Array.newInstance(rawClassOf(component), 0).getClass();
        }
        if (type instanceof WildcardType) {
            Type[] upper = ((WildcardType) type).getUpperBounds();
            if (upper.length > 0) return rawClassOf(upper[0]);
        }
        return Object.class;
    }

    private static String describe(JsonElement element) {
        if (element.isJsonObject()) return "a JSON Object";
        if (element.isJsonArray()) return "a JSON Array";
        if (element.isJsonPrimitive()) return "a JSON Primitive";
        return "JSON null";
    }
}

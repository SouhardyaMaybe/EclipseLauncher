package me.shadow.eclipselauncher.json;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A JSON object: an ordered map of member names to values. Insertion order is
 * preserved so that serialized output is stable.
 */
public final class JsonObject extends JsonElement {

    private final LinkedHashMap<String, JsonElement> members = new LinkedHashMap<>();

    public void add(String property, JsonElement value) {
        members.put(property, value == null ? JsonNull.INSTANCE : value);
    }

    public void addProperty(String property, String value) {
        add(property, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public void addProperty(String property, Number value) {
        add(property, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public void addProperty(String property, Boolean value) {
        add(property, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public void addProperty(String property, Character value) {
        add(property, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public JsonElement get(String memberName) {
        return members.get(memberName);
    }

    public boolean has(String memberName) {
        return members.containsKey(memberName);
    }

    public JsonElement remove(String memberName) {
        return members.remove(memberName);
    }

    public JsonPrimitive getAsJsonPrimitive(String memberName) {
        return (JsonPrimitive) members.get(memberName);
    }

    public JsonArray getAsJsonArray(String memberName) {
        return (JsonArray) members.get(memberName);
    }

    public JsonObject getAsJsonObject(String memberName) {
        return (JsonObject) members.get(memberName);
    }

    public Set<Map.Entry<String, JsonElement>> entrySet() {
        return members.entrySet();
    }

    public Set<String> keySet() {
        return members.keySet();
    }

    public int size() {
        return members.size();
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        JsonEmitter.write(this, builder, 0, false, false);
        return builder.toString();
    }
}

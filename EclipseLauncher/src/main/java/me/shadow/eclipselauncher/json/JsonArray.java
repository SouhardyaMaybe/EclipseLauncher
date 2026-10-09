package me.shadow.eclipselauncher.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A JSON array: an ordered list of elements. Iteration order is insertion order.
 */
public final class JsonArray extends JsonElement implements Iterable<JsonElement> {

    private final ArrayList<JsonElement> elements = new ArrayList<>();

    public void add(JsonElement element) {
        elements.add(element == null ? JsonNull.INSTANCE : element);
    }

    public void add(String value) {
        add(value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public void add(Boolean value) {
        add(value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public void add(Number value) {
        add(value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public void add(Character value) {
        add(value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
    }

    public JsonElement set(int index, JsonElement element) {
        return elements.set(index, element == null ? JsonNull.INSTANCE : element);
    }

    public boolean contains(JsonElement element) {
        return elements.contains(element);
    }

    public JsonElement get(int i) {
        return elements.get(i);
    }

    public int size() {
        return elements.size();
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public Iterator<JsonElement> iterator() {
        return elements.iterator();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        JsonEmitter.write(this, builder, 0, false, false);
        return builder.toString();
    }
}

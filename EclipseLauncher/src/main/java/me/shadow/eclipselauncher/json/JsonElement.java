package me.shadow.eclipselauncher.json;

/**
 * Base class of the JSON tree model. A parsed JSON document is a tree of
 * {@link JsonObject}, {@link JsonArray}, {@link JsonPrimitive} and {@link JsonNull} instances.
 *
 * <p>The API mirrors the subset of the tree model that the launcher call sites rely on,
 * so migrating away from the previous JSON library only required import changes.</p>
 */
public abstract class JsonElement {

    public boolean isJsonObject() {
        return this instanceof JsonObject;
    }

    public boolean isJsonArray() {
        return this instanceof JsonArray;
    }

    public boolean isJsonPrimitive() {
        return this instanceof JsonPrimitive;
    }

    public boolean isJsonNull() {
        return this instanceof JsonNull;
    }

    public JsonObject getAsJsonObject() {
        if (this instanceof JsonObject) return (JsonObject) this;
        throw new IllegalStateException("Not a JSON Object: " + this);
    }

    public JsonArray getAsJsonArray() {
        if (this instanceof JsonArray) return (JsonArray) this;
        throw new IllegalStateException("Not a JSON Array: " + this);
    }

    public JsonPrimitive getAsJsonPrimitive() {
        if (this instanceof JsonPrimitive) return (JsonPrimitive) this;
        throw new IllegalStateException("Not a JSON Primitive: " + this);
    }

    public JsonNull getAsJsonNull() {
        if (this instanceof JsonNull) return (JsonNull) this;
        throw new IllegalStateException("Not a JSON Null: " + this);
    }

    public String getAsString() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public Number getAsNumber() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public double getAsDouble() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public float getAsFloat() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public long getAsLong() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public int getAsInt() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public short getAsShort() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public byte getAsByte() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public char getAsChar() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public boolean getAsBoolean() {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    /** Compact JSON representation of this element (no pretty printing, no HTML escaping). */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        JsonEmitter.write(this, builder, 0, false, false);
        return builder.toString();
    }
}

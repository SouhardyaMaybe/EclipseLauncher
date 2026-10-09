package me.shadow.eclipselauncher.json;

/** The JSON {@code null} value. A single instance is shared by the whole process. */
public final class JsonNull extends JsonElement {

    public static final JsonNull INSTANCE = new JsonNull();

    private JsonNull() {
    }

    /** Kept for source compatibility: the serialized form is always {@code null}. */
    @Override
    public String toString() {
        return "null";
    }
}

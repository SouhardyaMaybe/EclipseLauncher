package me.shadow.eclipselauncher.json;

/**
 * A JSON string, number or boolean value. Numbers parsed from a document keep
 * their raw text so no precision is lost before the value is actually consumed.
 */
public final class JsonPrimitive extends JsonElement {

    /** Shared instances for the two boolean values. */
    public static final JsonPrimitive TRUE = new JsonPrimitive(Boolean.TRUE);
    public static final JsonPrimitive FALSE = new JsonPrimitive(Boolean.FALSE);

    /** String, Boolean or Number. Parsed numbers are kept as {@link LazilyParsedNumber}. */
    final Object value;

    public JsonPrimitive(Boolean bool) {
        if (bool == null) throw new NullPointerException("bool");
        this.value = bool;
    }

    public JsonPrimitive(Number number) {
        if (number == null) throw new NullPointerException("number");
        this.value = number;
    }

    public JsonPrimitive(String string) {
        if (string == null) throw new NullPointerException("string");
        this.value = string;
    }

    public JsonPrimitive(Character c) {
        if (c == null) throw new NullPointerException("character");
        this.value = c.toString();
    }

    public boolean isString() {
        return value instanceof String;
    }

    public boolean isNumber() {
        return value instanceof Number;
    }

    public boolean isBoolean() {
        return value instanceof Boolean;
    }

    @Override
    public String getAsString() {
        if (value instanceof String) return (String) value;
        if (value instanceof Number) return String.valueOf(value);
        return ((Boolean) value) ? "true" : "false";
    }

    @Override
    public Number getAsNumber() {
        if (value instanceof Number) return (Number) value;
        return new LazilyParsedNumber(getAsString());
    }

    @Override
    public double getAsDouble() {
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(getAsString());
    }

    @Override
    public float getAsFloat() {
        if (value instanceof Number) return ((Number) value).floatValue();
        return Float.parseFloat(getAsString());
    }

    @Override
    public long getAsLong() {
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(getAsString());
    }

    @Override
    public int getAsInt() {
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(getAsString());
    }

    @Override
    public short getAsShort() {
        if (value instanceof Number) return ((Number) value).shortValue();
        return Short.parseShort(getAsString());
    }

    @Override
    public byte getAsByte() {
        if (value instanceof Number) return ((Number) value).byteValue();
        return Byte.parseByte(getAsString());
    }

    @Override
    public char getAsChar() {
        if (value instanceof Number) return (char) ((Number) value).intValue();
        return getAsString().charAt(0);
    }

    @Override
    public boolean getAsBoolean() {
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(getAsString());
    }
}

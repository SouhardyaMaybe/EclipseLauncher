package me.shadow.eclipselauncher.json;

/**
 * Internal JSON writer: serializes tree elements to text, either compact or
 * pretty-printed with a two-space indent. String escaping matches the previous
 * library's default output (standard JSON escapes, optionally plus the five
 * HTML-sensitive characters {@code < > & = '}).
 */
final class JsonEmitter {

    private JsonEmitter() {
    }

    static void write(JsonElement element, StringBuilder out, int indent, boolean pretty, boolean htmlSafe) {
        if (element == null || element.isJsonNull()) {
            out.append("null");
        } else if (element.isJsonPrimitive()) {
            writePrimitive((JsonPrimitive) element, out, htmlSafe);
        } else if (element.isJsonArray()) {
            writeArray((JsonArray) element, out, indent, pretty, htmlSafe);
        } else {
            writeObject(element.getAsJsonObject(), out, indent, pretty, htmlSafe);
        }
    }

    private static void writePrimitive(JsonPrimitive primitive, StringBuilder out, boolean htmlSafe) {
        if (primitive.isString()) {
            writeString(primitive.getAsString(), out, htmlSafe);
        } else if (primitive.isBoolean()) {
            out.append(primitive.getAsBoolean() ? "true" : "false");
        } else if (primitive.value instanceof RawNumber) {
            // Raw number text parsed from a document, emitted verbatim
            out.append(primitive.value.toString());
        } else {
            Number number = primitive.getAsNumber();
            if (number == null) {
                out.append("null");
            } else {
                out.append(number.toString());
            }
        }
    }

    private static void writeArray(JsonArray array, StringBuilder out, int indent, boolean pretty, boolean htmlSafe) {
        if (array.size() == 0) {
            out.append("[]");
            return;
        }
        out.append('[');
        boolean first = true;
        for (JsonElement element : array) {
            if (!first) out.append(',');
            first = false;
            newlineAndIndent(out, indent + 1, pretty);
            write(element, out, indent + 1, pretty, htmlSafe);
        }
        newlineAndIndent(out, indent, pretty);
        out.append(']');
    }

    private static void writeObject(JsonObject object, StringBuilder out, int indent, boolean pretty, boolean htmlSafe) {
        if (object.size() == 0) {
            out.append("{}");
            return;
        }
        out.append('{');
        boolean first = true;
        for (java.util.Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!first) out.append(',');
            first = false;
            newlineAndIndent(out, indent + 1, pretty);
            writeString(entry.getKey(), out, htmlSafe);
            out.append(pretty ? ": " : ":");
            write(entry.getValue(), out, indent + 1, pretty, htmlSafe);
        }
        newlineAndIndent(out, indent, pretty);
        out.append('}');
    }

    private static void newlineAndIndent(StringBuilder out, int indent, boolean pretty) {
        if (!pretty) return;
        out.append('\n');
        for (int i = 0; i < indent; i++) {
            out.append("  ");
        }
    }

    static void writeString(String value, StringBuilder out, boolean htmlSafe) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                default:
                    if (c < 0x20 || (htmlSafe && isHtmlSensitive(c))) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                    break;
            }
        }
        out.append('"');
    }

    private static boolean isHtmlSensitive(char c) {
        return c == '<' || c == '>' || c == '&' || c == '=' || c == '\'';
    }
}

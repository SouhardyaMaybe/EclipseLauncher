package me.shadow.eclipselauncher.json

/**
 * Internal JSON writer: serializes tree elements to text, either compact or
 * pretty-printed with a two-space indent. String escaping matches the previous
 * library's default output (standard JSON escapes, optionally plus the five
 * HTML-sensitive characters < > & = ').
 */
internal object JsonEmitter {

    fun write(element: JsonElement?, out: StringBuilder, indent: Int, pretty: Boolean, htmlSafe: Boolean) {
        if (element == null || element.isJsonNull) {
            out.append("null")
        } else if (element.isJsonPrimitive) {
            writePrimitive(element.asJsonPrimitive, out, htmlSafe)
        } else if (element.isJsonArray) {
            writeArray(element.asJsonArray, out, indent, pretty, htmlSafe)
        } else {
            writeObject(element.asJsonObject, out, indent, pretty, htmlSafe)
        }
    }

    private fun writePrimitive(primitive: JsonPrimitive, out: StringBuilder, htmlSafe: Boolean) {
        if (primitive.isString) {
            writeString(primitive.asString, out, htmlSafe)
        } else if (primitive.isBoolean) {
            out.append(if (primitive.asBoolean) "true" else "false")
        } else if (primitive.value is RawNumber) {
            // Raw number text parsed from a document, emitted verbatim
            out.append(primitive.value.toString())
        } else {
            val number = primitive.asNumber
            if (number == null) {
                out.append("null")
            } else {
                out.append(number.toString())
            }
        }
    }

    private fun writeObject(jsonObject: JsonObject, out: StringBuilder, indent: Int, pretty: Boolean, htmlSafe: Boolean) {
        if (jsonObject.size() == 0) {
            out.append("{}")
            return
        }
        out.append('{')
        var first = true
        for (entry in jsonObject.entrySet()) {
            if (!first) out.append(',')
            first = false
            newlineAndIndent(out, indent + 1, pretty)
            writeString(entry.key, out, htmlSafe)
            out.append(if (pretty) ": " else ":")
            write(entry.value, out, indent + 1, pretty, htmlSafe)
        }
        newlineAndIndent(out, indent, pretty)
        out.append('}')
    }

    private fun writeArray(array: JsonArray, out: StringBuilder, indent: Int, pretty: Boolean, htmlSafe: Boolean) {
        if (array.size() == 0) {
            out.append("[]")
            return
        }
        out.append('[')
        var first = true
        for (element in array) {
            if (!first) out.append(',')
            first = false
            newlineAndIndent(out, indent + 1, pretty)
            write(element, out, indent + 1, pretty, htmlSafe)
        }
        newlineAndIndent(out, indent, pretty)
        out.append(']')
    }

    private fun newlineAndIndent(out: StringBuilder, indent: Int, pretty: Boolean) {
        if (!pretty) return
        out.append('\n')
        for (i in 0 until indent) {
            out.append("  ")
        }
    }

    fun writeString(value: String, out: StringBuilder, htmlSafe: Boolean) {
        out.append('"')
        for (i in 0 until value.length) {
            val c = value[i]
            when (c) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\b' -> out.append("\\b")
                '\t' -> out.append("\\t")
                '\n' -> out.append("\\n")
                '\u000C' -> out.append("\\f")
                '\r' -> out.append("\\r")
                else -> if (c.code < 0x20 || (htmlSafe && isHtmlSensitive(c))) {
                    out.append(java.lang.String.format("\\u%04x", c.code))
                } else {
                    out.append(c)
                }
            }
        }
        out.append('"')
    }

    private fun isHtmlSensitive(c: Char): Boolean {
        return c == '<' || c == '>' || c == '&' || c == '=' || c == '\''
    }
}

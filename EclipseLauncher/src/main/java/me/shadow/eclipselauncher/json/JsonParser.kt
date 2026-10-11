package me.shadow.eclipselauncher.json

import java.io.IOException
import java.io.Reader
import java.io.StringReader

/**
 * Lenient recursive-descent JSON parser producing the [JsonElement] tree.
 *
 * Lenient behavior matches the previous library's parser: unquoted field names,
 * single-quoted strings and the NaN/Infinity literals are accepted.
 */
object JsonParser {

    /** Parses a complete JSON document; trailing non-whitespace content is an error. */
    @JvmStatic
    fun parseString(json: String): JsonElement {
        return parseReader(StringReader(json))
    }

    /** Reads and parses a complete JSON document from the given reader. */
    @JvmStatic
    fun parseReader(reader: Reader): JsonElement {
        val parser = Parser(readFully(reader))
        val element = parser.parseValue()
        parser.skipWhitespace()
        if (!parser.atEnd()) {
            throw JsonIOException("JSON document was not fully consumed.")
        }
        return element
    }

    private fun readFully(reader: Reader): String {
        try {
            val builder = StringBuilder()
            val buffer = CharArray(8192)
            while (true) {
                val read = reader.read(buffer)
                if (read == -1) break
                builder.append(buffer, 0, read)
            }
            return builder.toString()
        } catch (e: IOException) {
            throw JsonIOException(e)
        }
    }

    private class Parser(private val json: String) {
        private var pos = 0

        fun atEnd(): Boolean {
            return pos >= json.length
        }

        private fun peek(): Char {
            if (atEnd()) throw JsonSyntaxException("Unexpected end of document")
            return json[pos]
        }

        private fun expect(expected: Char) {
            val c = peek()
            if (c != expected) {
                throw JsonSyntaxException("Expected '$expected' but was '$c' at position $pos")
            }
            pos++
        }

        fun skipWhitespace() {
            while (pos < json.length) {
                val c = json[pos]
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\u000C') pos++
                else break
            }
        }

        fun parseValue(): JsonElement {
            skipWhitespace()
            if (atEnd()) {
                // An empty document parses to JSON null, same as the previous library
                return JsonNull.INSTANCE
            }
            val c = peek()
            return when (c) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"', '\'' -> JsonPrimitive(parseQuotedString())
                't', 'f', 'n' -> parseKeyword()
                else -> parseNumberOrLiteral()
            }
        }

        private fun parseObject(): JsonObject {
            expect('{')
            val jsonObject = JsonObject()
            skipWhitespace()
            if (!atEnd() && peek() == '}') {
                pos++
                return jsonObject
            }
            while (true) {
                skipWhitespace()
                val name = parseName()
                skipWhitespace()
                expect(':')
                jsonObject.add(name, parseValue())
                skipWhitespace()
                if (atEnd()) throw JsonSyntaxException("Unterminated object at position $pos")
                val c = peek()
                if (c == ',') {
                    pos++
                } else if (c == '}') {
                    pos++
                    return jsonObject
                } else {
                    throw JsonSyntaxException("Expected ',' or '}' but was '$c' at position $pos")
                }
            }
        }

        private fun parseArray(): JsonArray {
            expect('[')
            val array = JsonArray()
            skipWhitespace()
            if (!atEnd() && peek() == ']') {
                pos++
                return array
            }
            while (true) {
                array.add(parseValue())
                skipWhitespace()
                if (atEnd()) throw JsonSyntaxException("Unterminated array at position $pos")
                val c = peek()
                if (c == ',') {
                    pos++
                } else if (c == ']') {
                    pos++
                    return array
                } else {
                    throw JsonSyntaxException("Expected ',' or ']' but was '$c' at position $pos")
                }
            }
        }

        /** Field name: quoted string (either quote style) or, in lenient mode, unquoted text. */
        private fun parseName(): String {
            val c = peek()
            if (c == '"' || c == '\'') return parseQuotedString()
            // Lenient: unquoted name, terminated like the previous library's unquoted values
            val start = pos
            while (pos < json.length) {
                val u = json[pos]
                if (u == ':' || u == ',' || u == '}' || u == ']' || u == '"' || u == '\'' || u == '\\' ||
                    u == '#' || u == ' ' || u == '\t' || u == '\n' || u == '\r' || u == '\u000C'
                ) {
                    break
                }
                pos++
            }
            if (pos == start) throw JsonSyntaxException("Expected a field name at position $pos")
            return json.substring(start, pos)
        }

        private fun parseQuotedString(): String {
            val quote = peek()
            pos++
            val builder = StringBuilder()
            while (true) {
                if (atEnd()) throw JsonSyntaxException("Unterminated string")
                val c = json[pos++]
                if (c == quote) return builder.toString()
                if (c == '\\') {
                    if (atEnd()) throw JsonSyntaxException("Unterminated string escape")
                    val escape = json[pos++]
                    when (escape) {
                        '"' -> builder.append('"')
                        '\'' -> builder.append('\'')
                        '\\' -> builder.append('\\')
                        '/' -> builder.append('/')
                        'b' -> builder.append('\b')
                        't' -> builder.append('\t')
                        'n' -> builder.append('\n')
                        'f' -> builder.append('\u000C')
                        'r' -> builder.append('\r')
                        'u' -> {
                            if (pos + 4 > json.length) throw JsonSyntaxException("Unterminated unicode escape")
                            try {
                                builder.append(json.substring(pos, pos + 4).toInt(16).toChar())
                            } catch (e: NumberFormatException) {
                                throw JsonSyntaxException("Invalid unicode escape at position $pos", e)
                            }
                            pos += 4
                        }
                        else -> throw JsonSyntaxException("Invalid escape character '\\$escape' at position $pos")
                    }
                } else if (c.code < 0x20) {
                    throw JsonSyntaxException("Unescaped control character (0x${java.lang.Integer.toHexString(c.code)}) in string")
                } else {
                    builder.append(c)
                }
            }
        }

        private fun parseKeyword(): JsonElement {
            if (matchLiteral("true")) return JsonPrimitive.TRUE
            if (matchLiteral("false")) return JsonPrimitive.FALSE
            if (matchLiteral("null")) return JsonNull.INSTANCE
            // Lenient: names may start with a letter but be plain unquoted values
            return parseNumberOrLiteral()
        }

        private fun matchLiteral(literal: String): Boolean {
            if (json.regionMatches(pos, literal, 0, literal.length)) {
                pos += literal.length
                return true
            }
            return false
        }

        /**
         * Lenient number or literal scan: collects the raw text (digits, signs,
         * exponents and, in lenient mode, letters) and interprets it afterwards.
         */
        private fun parseNumberOrLiteral(): JsonElement {
            val start = pos
            while (pos < json.length) {
                val c = json[pos]
                if (c >= '0' && c <= '9' || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E' ||
                    c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'
                ) {
                    pos++
                } else {
                    break
                }
            }
            if (pos == start) {
                throw JsonSyntaxException("Unexpected character '${peek()}' at position $pos")
            }
            val text = json.substring(start, pos)
            return when (text) {
                "NaN" -> JsonPrimitive(Double.NaN)
                "Infinity" -> JsonPrimitive(Double.POSITIVE_INFINITY)
                "+Infinity" -> JsonPrimitive(Double.POSITIVE_INFINITY)
                "-Infinity" -> JsonPrimitive(Double.NEGATIVE_INFINITY)
                else -> if (isNumeric(text)) {
                    JsonPrimitive(RawNumber(text))
                } else {
                    // Lenient unquoted value: behaves like a plain string primitive
                    JsonPrimitive(text)
                }
            }
        }

        companion object {
            private val NUMERIC_PATTERN =
                "[-+]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][-+]?\\d+)?|[-+]?0[xX][0-9a-fA-F]+"

            private fun isNumeric(text: String): Boolean {
                // Regex.matches requires a full match, exactly like java's String.matches
                return Regex(NUMERIC_PATTERN).matches(text)
            }
        }
    }
}

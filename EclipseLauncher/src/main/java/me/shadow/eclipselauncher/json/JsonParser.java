package me.shadow.eclipselauncher.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Lenient recursive-descent JSON parser producing the {@link JsonElement} tree.
 *
 * <p>Lenient behavior matches the previous library's parser: unquoted field names,
 * single-quoted strings and the {@code NaN}/{@code Infinity} literals are accepted.</p>
 */
public final class JsonParser {

    private JsonParser() {
    }

    /** Parses a complete JSON document; trailing non-whitespace content is an error. */
    public static JsonElement parseString(String json) throws JsonSyntaxException, JsonIOException {
        return parseReader(new StringReader(json));
    }

    /** Reads and parses a complete JSON document from the given reader. */
    public static JsonElement parseReader(Reader reader) throws JsonSyntaxException, JsonIOException {
        Parser parser = new Parser(readFully(reader));
        JsonElement element = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw new JsonIOException("JSON document was not fully consumed.");
        }
        return element;
    }

    private static String readFully(Reader reader) throws JsonIOException {
        try {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        } catch (IOException e) {
            throw new JsonIOException(e);
        }
    }

    private static final class Parser {

        private final String json;
        private int pos;

        Parser(String json) {
            this.json = json;
        }

        boolean atEnd() {
            return pos >= json.length();
        }

        private char peek() throws JsonSyntaxException {
            if (atEnd()) throw new JsonSyntaxException("Unexpected end of document");
            return json.charAt(pos);
        }

        private void expect(char expected) throws JsonSyntaxException {
            char c = peek();
            if (c != expected) {
                throw new JsonSyntaxException("Expected '" + expected + "' but was '" + c + "' at position " + pos);
            }
            pos++;
        }

        void skipWhitespace() {
            while (pos < json.length()) {
                char c = json.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f') pos++;
                else break;
            }
        }

        JsonElement parseValue() throws JsonSyntaxException {
            skipWhitespace();
            if (atEnd()) {
                // An empty document parses to JSON null, same as the previous library
                return JsonNull.INSTANCE;
            }
            char c = peek();
            switch (c) {
                case '{':
                    return parseObject();
                case '[':
                    return parseArray();
                case '"':
                case '\'':
                    return new JsonPrimitive(parseQuotedString());
                case 't':
                case 'f':
                case 'n':
                    return parseKeyword();
                default:
                    return parseNumberOrLiteral();
            }
        }

        private JsonObject parseObject() throws JsonSyntaxException {
            expect('{');
            JsonObject object = new JsonObject();
            skipWhitespace();
            if (!atEnd() && peek() == '}') {
                pos++;
                return object;
            }
            while (true) {
                skipWhitespace();
                String name = parseName();
                skipWhitespace();
                expect(':');
                object.add(name, parseValue());
                skipWhitespace();
                if (atEnd()) throw new JsonSyntaxException("Unterminated object at position " + pos);
                char c = peek();
                if (c == ',') {
                    pos++;
                } else if (c == '}') {
                    pos++;
                    return object;
                } else {
                    throw new JsonSyntaxException("Expected ',' or '}' but was '" + c + "' at position " + pos);
                }
            }
        }

        private JsonArray parseArray() throws JsonSyntaxException {
            expect('[');
            JsonArray array = new JsonArray();
            skipWhitespace();
            if (!atEnd() && peek() == ']') {
                pos++;
                return array;
            }
            while (true) {
                array.add(parseValue());
                skipWhitespace();
                if (atEnd()) throw new JsonSyntaxException("Unterminated array at position " + pos);
                char c = peek();
                if (c == ',') {
                    pos++;
                } else if (c == ']') {
                    pos++;
                    return array;
                } else {
                    throw new JsonSyntaxException("Expected ',' or ']' but was '" + c + "' at position " + pos);
                }
            }
        }

        /** Field name: quoted string (either quote style) or, in lenient mode, unquoted text. */
        private String parseName() throws JsonSyntaxException {
            char c = peek();
            if (c == '"' || c == '\'') return parseQuotedString();
            // Lenient: unquoted name, terminated like the previous library's unquoted values
            int start = pos;
            while (pos < json.length()) {
                char u = json.charAt(pos);
                if (u == ':' || u == ',' || u == '}' || u == ']' || u == '"' || u == '\'' || u == '\\'
                        || u == '#' || u == ' ' || u == '\t' || u == '\n' || u == '\r' || u == '\f') {
                    break;
                }
                pos++;
            }
            if (pos == start) throw new JsonSyntaxException("Expected a field name at position " + pos);
            return json.substring(start, pos);
        }

        private String parseQuotedString() throws JsonSyntaxException {
            char quote = peek();
            pos++;
            StringBuilder builder = new StringBuilder();
            while (true) {
                if (atEnd()) throw new JsonSyntaxException("Unterminated string");
                char c = json.charAt(pos++);
                if (c == quote) return builder.toString();
                if (c == '\\') {
                    if (atEnd()) throw new JsonSyntaxException("Unterminated string escape");
                    char escape = json.charAt(pos++);
                    switch (escape) {
                        case '"': builder.append('"'); break;
                        case '\'': builder.append('\''); break;
                        case '\\': builder.append('\\'); break;
                        case '/': builder.append('/'); break;
                        case 'b': builder.append('\b'); break;
                        case 't': builder.append('\t'); break;
                        case 'n': builder.append('\n'); break;
                        case 'f': builder.append('\f'); break;
                        case 'r': builder.append('\r'); break;
                        case 'u':
                            if (pos + 4 > json.length()) throw new JsonSyntaxException("Unterminated unicode escape");
                            try {
                                builder.append((char) Integer.parseInt(json.substring(pos, pos + 4), 16));
                            } catch (NumberFormatException e) {
                                throw new JsonSyntaxException("Invalid unicode escape at position " + pos, e);
                            }
                            pos += 4;
                            break;
                        default:
                            throw new JsonSyntaxException("Invalid escape character '\\" + escape + "' at position " + pos);
                    }
                } else if (c < 0x20) {
                    throw new JsonSyntaxException("Unescaped control character (0x" + Integer.toHexString(c) + ") in string");
                } else {
                    builder.append(c);
                }
            }
        }

        private JsonElement parseKeyword() throws JsonSyntaxException {
            if (matchLiteral("true")) return JsonPrimitive.TRUE;
            if (matchLiteral("false")) return JsonPrimitive.FALSE;
            if (matchLiteral("null")) return JsonNull.INSTANCE;
            // Lenient: names may start with a letter but be plain unquoted values
            return parseNumberOrLiteral();
        }

        private boolean matchLiteral(String literal) {
            if (json.regionMatches(pos, literal, 0, literal.length())) {
                pos += literal.length();
                return true;
            }
            return false;
        }

        /**
         * Lenient number or literal scan: collects the raw text (digits, signs,
         * exponents and, in lenient mode, letters) and interprets it afterwards.
         */
        private JsonElement parseNumberOrLiteral() throws JsonSyntaxException {
            int start = pos;
            while (pos < json.length()) {
                char c = json.charAt(pos);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E'
                        || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                    pos++;
                } else {
                    break;
                }
            }
            if (pos == start) {
                throw new JsonSyntaxException("Unexpected character '" + peek() + "' at position " + pos);
            }
            String text = json.substring(start, pos);
            switch (text) {
                case "NaN":
                    return new JsonPrimitive(Double.NaN);
                case "Infinity":
                    return new JsonPrimitive(Double.POSITIVE_INFINITY);
                case "+Infinity":
                    return new JsonPrimitive(Double.POSITIVE_INFINITY);
                case "-Infinity":
                    return new JsonPrimitive(Double.NEGATIVE_INFINITY);
                default:
                    if (isNumeric(text)) {
                        return new JsonPrimitive(new RawNumber(text));
                    }
                    // Lenient unquoted value: behaves like a plain string primitive
                    return new JsonPrimitive(text);
            }
        }

        private static final String NUMERIC_PATTERN =
                "[-+]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][-+]?\\d+)?|[-+]?0[xX][0-9a-fA-F]+";

        private static boolean isNumeric(String text) {
            return text.matches(NUMERIC_PATTERN);
        }
    }
}

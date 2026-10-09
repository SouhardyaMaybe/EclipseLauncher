package me.shadow.eclipselauncher.json;

/** Thrown when a JSON document is malformed or a value cannot be mapped to the requested type. */
public class JsonSyntaxException extends JsonParseException {

    public JsonSyntaxException(String msg) {
        super(msg);
    }

    public JsonSyntaxException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public JsonSyntaxException(Throwable cause) {
        super(cause);
    }
}

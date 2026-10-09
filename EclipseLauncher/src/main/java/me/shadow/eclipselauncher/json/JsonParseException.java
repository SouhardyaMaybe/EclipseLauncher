package me.shadow.eclipselauncher.json;

/** Runtime exception thrown for any problem while parsing or mapping JSON. */
public class JsonParseException extends RuntimeException {

    public JsonParseException(String msg) {
        super(msg);
    }

    public JsonParseException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public JsonParseException(Throwable cause) {
        super(cause);
    }
}

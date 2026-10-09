package me.shadow.eclipselauncher.json;

/** Thrown when an I/O problem occurs while reading or writing a JSON document. */
public class JsonIOException extends JsonParseException {

    public JsonIOException(String msg) {
        super(msg);
    }

    public JsonIOException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public JsonIOException(Throwable cause) {
        super(cause);
    }
}

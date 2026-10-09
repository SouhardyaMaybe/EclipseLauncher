package me.shadow.eclipselauncher.json;

/** Configures and creates {@link JsonCodec} instances. */
public final class JsonCodecBuilder {

    private boolean prettyPrinting;
    private boolean htmlSafe = true;

    /** Enables pretty printing with a two-space indent. */
    public JsonCodecBuilder setPrettyPrinting() {
        prettyPrinting = true;
        return this;
    }

    /** Disables escaping of the HTML-sensitive characters {@code < > & = '}. */
    public JsonCodecBuilder disableHtmlEscaping() {
        htmlSafe = false;
        return this;
    }

    public JsonCodec create() {
        return new JsonCodec(htmlSafe, prettyPrinting);
    }
}

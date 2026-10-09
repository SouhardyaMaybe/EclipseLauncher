package me.shadow.eclipselauncher.json;

/** Configures and creates {@link Gson} instances. */
public final class GsonBuilder {

    private boolean prettyPrinting;
    private boolean htmlSafe = true;

    /** Enables pretty printing with a two-space indent. */
    public GsonBuilder setPrettyPrinting() {
        prettyPrinting = true;
        return this;
    }

    /** Disables escaping of the HTML-sensitive characters {@code < > & = '}. */
    public GsonBuilder disableHtmlEscaping() {
        htmlSafe = false;
        return this;
    }

    public Gson create() {
        return new Gson(htmlSafe, prettyPrinting);
    }
}

package me.shadow.eclipselauncher.json

/** Configures and creates [JsonCodec] instances. */
class JsonCodecBuilder {

    private var prettyPrinting = false
    private var htmlSafe = true

    /** Enables pretty printing with a two-space indent. */
    fun setPrettyPrinting(): JsonCodecBuilder {
        prettyPrinting = true
        return this
    }

    /** Disables escaping of the HTML-sensitive characters < > & = '. */
    fun disableHtmlEscaping(): JsonCodecBuilder {
        htmlSafe = false
        return this
    }

    fun create(): JsonCodec {
        return JsonCodec(htmlSafe, prettyPrinting)
    }
}

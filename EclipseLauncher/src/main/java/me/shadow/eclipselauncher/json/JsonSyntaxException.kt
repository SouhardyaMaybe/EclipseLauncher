package me.shadow.eclipselauncher.json

/** Raised when the JSON document does not follow the grammar the parser expects. */
class JsonSyntaxException : JsonParseException {

    constructor(msg: String) : super(msg)

    constructor(msg: String, cause: Throwable) : super(msg, cause)

    constructor(cause: Throwable) : super(cause)
}

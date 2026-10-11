package me.shadow.eclipselauncher.json

/** Raised when JSON processing fails due to an I/O problem. */
class JsonIOException : JsonParseException {

    constructor(msg: String) : super(msg)

    constructor(msg: String, cause: Throwable) : super(msg, cause)

    constructor(cause: Throwable) : super(cause)
}

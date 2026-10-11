package me.shadow.eclipselauncher.json

/** Raised for any problem met while reading or writing JSON. */
open class JsonParseException : RuntimeException {

    constructor(msg: String) : super(msg)

    constructor(msg: String, cause: Throwable) : super(msg, cause)

    constructor(cause: Throwable) : super(cause)
}

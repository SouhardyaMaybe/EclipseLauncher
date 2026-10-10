package me.shadow.eclipselauncher.utils.file

class InvalidFilenameException : RuntimeException {

    private val type: FilenameErrorType

    val illegalCharacters: String?

    val invalidLength: Int

    val isInvalidLength: Boolean
        get() = type == FilenameErrorType.INVALID_LENGTH

    constructor(message: String?, illegalCharacters: String?) : super(message) {
        this.type = FilenameErrorType.CONTAINS_ILLEGAL_CHARACTERS
        this.illegalCharacters = illegalCharacters
        this.invalidLength = -1
    }

    constructor(message: String?, invalidLength: Int) : super(message) {
        this.type = FilenameErrorType.INVALID_LENGTH
        this.illegalCharacters = null
        this.invalidLength = invalidLength
    }

    fun containsIllegalCharacters(): Boolean = type == FilenameErrorType.CONTAINS_ILLEGAL_CHARACTERS

    private enum class FilenameErrorType {
        CONTAINS_ILLEGAL_CHARACTERS,
        INVALID_LENGTH
    }
}

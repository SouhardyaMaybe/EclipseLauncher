package me.shadow.eclipselauncher.utils.version

/**
 * Ordering and canonicalization for Minecraft-style version strings.
 *
 * A version is split wherever its character class changes: digit runs become
 * numbers, letter runs become words, and every punctuation character (., -,
 * _, ...) acts as a separator. Two adjacent separators contribute an
 * implicit 0 between them (and a leading separator a 0 in front), while
 * trailing separators are dropped.
 *
 * [compare] walks the canonical token lists side by side:
 *
 *  - numbers compare by value, so `1.9` < `1.12` and `1.20.2` < `1.20.10`,
 *  - words compare lexically, and a word ranks below a number at the same
 *    position (so `1.20-pre1` sorts before `1.20.1`, but a numeric suffix
 *    such as `1.20.1` sorts above the bare `1.20`),
 *  - trailing zero tokens are stripped first, so `1.10.0` == `1.10`,
 *  - when one list runs out, the longer one is greater, so `1.20` <
 *    `1.20.1` and `1.20-pre1` ranks above `1.20`.
 *
 * [normalize] renders the canonical tokens back into text using the
 * separators of the original string: `1.10.0` becomes `1.10`, `09` becomes
 * `9`, and `24w09a` becomes `24w9a`.
 */
object VersionNumbers {

    /**
     * Orders two version strings: negative when [one] sorts before [two],
     * zero when both denote the same version, positive otherwise.
     */
    fun compare(one: String, two: String): Int {
        val left = parse(one).tokens
        val right = parse(two).tokens
        val shared = minOf(left.size, right.size)
        for (index in 0 until shared) {
            val result = compareTokens(left[index], right[index])
            if (result != 0) return result
        }
        return left.size.compareTo(right.size)
    }

    /** Canonical text of [version]: no leading zeros, no trailing zero segments. */
    fun normalize(version: String): String = render(parse(version))

    private sealed class Token {
        class Number(val digits: String) : Token()
        class Word(val text: String) : Token()
    }

    private class Parsed(val separators: List<String>, val tokens: List<Token>)

    private fun parse(version: String): Parsed {
        val tokens = ArrayList<Token>()
        val separators = ArrayList<String>()
        // Separator text waiting to be attached to the next token, if any.
        var pending: String? = null

        var index = 0
        while (index < version.length) {
            val char = version[index]
            when {
                char.isDigit() -> {
                    val start = index
                    while (index < version.length && version[index].isDigit()) index++
                    appendToken(tokens, separators, Token.Number(version.substring(start, index)), pending)
                    pending = null
                }

                char.isLetter() -> {
                    val start = index
                    while (index < version.length && version[index].isLetter()) index++
                    appendToken(tokens, separators, Token.Word(version.substring(start, index)), pending)
                    pending = null
                }

                else -> {
                    if (pending == null) {
                        pending = char.toString()
                    } else {
                        // Two separators in a row mean the segment between
                        // them is empty, which counts as a 0.
                        appendToken(tokens, separators, Token.Number("0"), pending)
                        pending = char.toString()
                    }
                    index++
                }
            }
        }

        // Trailing separators carry no information.
        // Trailing zero segments are stripped so that 1.10.0 == 1.10.
        while (tokens.isNotEmpty() && isZero(tokens[tokens.lastIndex])) {
            tokens.removeAt(tokens.lastIndex)
            if (separators.isNotEmpty()) separators.removeAt(separators.lastIndex)
        }

        return Parsed(separators, tokens)
    }

    private fun appendToken(tokens: MutableList<Token>, separators: MutableList<String>, token: Token, pending: String?) {
        when {
            pending != null && tokens.isEmpty() -> {
                // A separator before the first token contributes a leading 0.
                tokens.add(Token.Number("0"))
                separators.add(pending)
                tokens.add(token)
            }

            pending != null -> {
                tokens.add(token)
                separators.add(pending)
            }

            tokens.isNotEmpty() -> {
                tokens.add(token)
                separators.add("")
            }

            else -> tokens.add(token)
        }
    }

    private fun isZero(token: Token): Boolean = token is Token.Number && stripLeadingZeros(token.digits).isEmpty()

    private fun compareTokens(left: Token, right: Token): Int = when {
        left is Token.Number && right is Token.Number -> compareDigits(left.digits, right.digits)
        left is Token.Word && right is Token.Word -> left.text.compareTo(right.text)
        left is Token.Word -> -1 // words rank below numbers
        else -> 1
    }

    private fun compareDigits(one: String, two: String): Int {
        val a = stripLeadingZeros(one)
        val b = stripLeadingZeros(two)
        return if (a.length != b.length) a.length.compareTo(b.length) else a.compareTo(b)
    }

    private fun stripLeadingZeros(digits: String): String {
        val firstNonZero = digits.indexOfFirst { it != '0' }
        return if (firstNonZero == -1) "" else digits.substring(firstNonZero)
    }

    private fun render(parsed: Parsed): String {
        val builder = StringBuilder()
        parsed.tokens.forEachIndexed { index, token ->
            if (index > 0) builder.append(parsed.separators[index - 1])
            builder.append(
                when (token) {
                    is Token.Number -> stripLeadingZeros(token.digits).ifEmpty { "0" }
                    is Token.Word -> token.text
                }
            )
        }
        return builder.toString()
    }
}

package me.shadow.eclipselauncher.utils.stringutils

import java.util.Locale
import java.util.regex.Pattern

class StringFilter {
    companion object {
        /**
         * Check whether the input string contains the specified substring.
         * @param input the input string
         * @param substring the substring to check for
         * @param caseSensitive whether to be case sensitive
         * @return true if the input string contains the specified substring, otherwise false
         */
        @JvmStatic
        fun containsSubstring(input: String, substring: String, caseSensitive: Boolean): Boolean {
            val adjustedInput = if (caseSensitive) input else input.lowercase(Locale.getDefault())
            val adjustedSubstring =
                if (caseSensitive) substring else substring.lowercase(Locale.getDefault())
            val regex = Pattern.quote(adjustedSubstring)
            val compiledPattern = Pattern.compile(regex)
            val matcher = compiledPattern.matcher(adjustedInput)
            return matcher.find()
        }
    }
}

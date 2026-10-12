package me.shadow.eclipselauncher.listener

import android.text.Editable
import android.text.TextWatcher

/**
 * Most interfaces implementations of [TextWatcher] only implement the afterTextChanged method.
 * This interface provides a default for other methods.
 */
interface SimpleTextWatcher : TextWatcher {
    override fun afterTextChanged(s: Editable?)

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
}

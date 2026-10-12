package me.shadow.eclipselauncher.core.customcontrols.keyboard


import android.content.Context
import android.text.Editable
import android.text.Selection
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import me.shadow.eclipselauncher.R

/**
 * This class is intended for sending characters used in chat via the virtual keyboard
 */
class TouchCharInput : androidx.appcompat.widget.AppCompatEditText {

    private var mIsDoingInternalChanges = false
    private var mCharacterSender: CharacterSenderStrategy? = null

    constructor(@NonNull context: Context) : this(context, null)

    constructor(@NonNull context: Context, @Nullable attrs: AttributeSet?) : this(context, attrs, R.attr.editTextStyle)

    constructor(@NonNull context: Context, @Nullable attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setup()
    }

    /**
     * When we change from app to app, the keyboard gets disabled.
     * So, we disable the object
     */
    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        disable()
    }

    /**
     * Intercepts the back key to disable focus
     * Does not affect the rest of the activity.
     */
    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            disable()
        }
        return super.onKeyPreIme(keyCode, event)
    }

    /**
     * Toggle on and off the soft keyboard, depending of the state
     */
    fun switchKeyboardState() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        // Allow, regardless of whether or not a hardware keyboard is declared
        if (hasFocus()) {
            clear()
            disable()
        } else {
            enable()
            imm!!.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    /**
     * Clear the EditText from any leftover inputs
     * It does not affect the in-game input
     */
    fun clear() {
        mIsDoingInternalChanges = true
        // Edit the Editable directly as it doesn't affect the state
        // of the TextView.
        val editable = editableText
        editable.clear()
        //Braille space, doesn't trigger keyboard auto-complete
        editable.append(TEXT_FILLER)
        Selection.setSelection(editable, TEXT_FILLER.length)
        mIsDoingInternalChanges = false
    }

    /** Regain ability to exist, take focus and have some text being input */
    fun enable() {
        isEnabled = true
        isFocusable = true
        visibility = View.VISIBLE
        requestFocus()
    }

    /** Lose ability to exist, take focus and have some text being input */
    fun disable() {
        clear()
        visibility = View.GONE
        clearFocus()
        isEnabled = false
        //setFocusable(false);
    }

    /** Send the enter key. */
    private fun sendEnter() {
        mCharacterSender!!.sendEnter()
        clear()
    }

    /** Just sets the char sender that should be used. */
    fun setCharacterSender(characterSender: CharacterSenderStrategy) {
        mCharacterSender = characterSender
    }

    /** This function deals with anything that has to be executed when the constructor is called */
    private fun setup() {
        // Using TextWatcher instead of overriding onTextChanged because some Huawei firmware
        // calls setText in constructor, causing havoc for our listener
        addTextChangedListener(InputTextWatcher())
        setOnEditorActionListener { _, _, _ ->
            sendEnter()
            clear()
            disable()
            false
        }
        clear()
        disable()
    }

    private inner class InputTextWatcher : android.text.TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
        }

        /**
         * We take the new chars, and send them to the game.
         * If less chars are present, remove some.
         * The text is always cleaned up.
         */
        override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
            if (mIsDoingInternalChanges) return
            val sender = mCharacterSender
            if (sender != null) {
                for (i in 0 until lengthBefore) {
                    sender.sendBackspace()
                }
                for (i in start until start + lengthAfter) {
                    sender.sendChar(text!![i])
                }
            }
        }

        override fun afterTextChanged(editable: Editable?) {
            if (mIsDoingInternalChanges) return
            // Moved from onTextChanged because "It is an error to attempt to make changes to s from this callback."
            // reference: https://developer.android.com/reference/android/text/TextWatcher#onTextChanged(java.lang.CharSequence,%20int,%20int,%20int)
            if (editable!!.length < 1) clear()
        }
    }

    companion object {
        const val TEXT_FILLER = "                              "
    }
}

package me.shadow.eclipselauncher.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.EditText
import androidx.annotation.CheckResult
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.databinding.DialogEditTextBinding
import me.shadow.eclipselauncher.ui.dialog.DraggableDialog.DialogInitializationListener
import me.shadow.eclipselauncher.utils.stringutils.StringUtilsKt.Companion.isEmptyOrBlank

class EditTextDialog private constructor(
    private val context: Context,
    private val title: String?,
    private val message: String?,
    private val editText: String?,
    private val hintText: String?,
    private val checkBox: String?,
    private val confirm: String?,
    private val emptyError: String?,
    private val showCheckBox: Boolean,
    private val inputType: Int,
    private val cancelListener: View.OnClickListener?,
    private val confirmListener: ConfirmListener?,
    private val showSkinCape: Boolean,
    private val skinListener: View.OnClickListener?,
    private val capeListener: View.OnClickListener?,
    private val required: Boolean
) : FullScreenDialog(context),
    DialogInitializationListener {
    private val binding = DialogEditTextBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setCancelable(false)
        this.setContentView(binding.root)

        init()
        DraggableDialog.initDialog(this)
    }

    private fun init() {
        binding.apply {
            title?.let { titleView.text = it }
            message?.let {
                messageView.text = it
                messageView.visibility = View.VISIBLE
            }
            editText?.let { textEdit.setText(it) }
            hintText?.let { textEdit.hint = it } ?: run {
                if (required) textEdit.setHint(R.string.generic_required)
            }

            if (showSkinCape) {
                skinCapeRow.visibility = View.VISIBLE
                skinListener?.let { setSkinButton.setOnClickListener(it) }
                capeListener?.let { setCapeButton.setOnClickListener(it) }
            }

            checkHeight()

            confirm?.let { confirmButton.text = it }
            if (showCheckBox) {
                checkBox.visibility = View.VISIBLE
                checkBox.text = this@EditTextDialog.checkBox
            }
            if (inputType != -1) textEdit.inputType = inputType

            confirmListener?.let {
                confirmButton.setOnClickListener { _ ->
                    if (required) {
                        val text = textEdit.text.toString()
                        if (isEmptyOrBlank(text)) {
                            textEdit.error = emptyError ?: context.getString(R.string.generic_error_field_empty)
                            return@setOnClickListener
                        }
                    }
                    val dismissDialog = it.onConfirm(textEdit, checkBox.isChecked)
                    if (dismissDialog) dismiss()
                }
            }

            val cancelListener = cancelListener ?: View.OnClickListener { dismiss() }
            cancelButton.setOnClickListener(cancelListener)
        }
    }

    private fun checkHeight() {
        checkHeight(binding.root, binding.contentView, binding.scrollView)
    }

    /** Toggles the "Set Skin" button between its plain and selected states. */
    fun markSkinSelected(selected: Boolean) {
        binding.setSkinButton.setText(
            if (selected) R.string.account_set_skin_selected else R.string.account_set_skin
        )
    }

    /** Toggles the "Set Cape" button between its plain and selected states. */
    fun markCapeSelected(selected: Boolean) {
        binding.setCapeButton.setText(
            if (selected) R.string.account_set_cape_selected else R.string.account_set_cape
        )
    }

    override fun onInit(): Window? = window

    fun interface ConfirmListener {
        fun onConfirm(editText: EditText, checked: Boolean): Boolean
    }

    class Builder(private val context: Context) {
        private var title: String? = null
        private var message: String? = null
        private var editText: String? = null
        private var hintText: String? = null
        private var checkBox: String? = null
        private var confirm: String? = null
        private var emptyError: String? = null
        private var showCheckBox = false
        private var inputType = -1
        private var cancelListener: View.OnClickListener? = null
        private var confirmListener: ConfirmListener? = null
        private var showSkinCape = false
        private var skinListener: View.OnClickListener? = null
        private var capeListener: View.OnClickListener? = null
        private var required = false

        /**
         * Set the dialog's title bar text
         */
        @CheckResult
        fun setTitle(title: String): Builder {
            this.title = title
            return this
        }

        /**
         * Set the dialog's title bar text
         */
        @CheckResult
        fun setTitle(title: Int): Builder {
            return setTitle(context.getString(title))
        }

        /**
         * Set the dialog's message text
         */
        @CheckResult
        fun setMessage(message: String): Builder {
            this.message = message
            return this
        }

        /**
         * Set the dialog's message text
         */
        @CheckResult
        fun setMessage(message: Int): Builder {
            return setMessage(context.getString(message))
        }

        /**
         * Set the text of the input field
         */
        @CheckResult
        fun setEditText(editText: String): Builder {
            this.editText = editText
            return this
        }

        /**
         * Set the hint of the input field
         */
        @CheckResult
        fun setHintText(hintText: Int): Builder {
            return setHintText(context.getString(hintText))
        }

        /**
         * Set the hint of the input field
         */
        @CheckResult
        fun setHintText(hintText: String): Builder {
            this.hintText = hintText
            return this
        }

        /**
         * Set the text of the confirm button
         */
        @CheckResult
        fun setConfirmText(text: Int): Builder {
            return setConfirmText(context.getString(text))
        }

        /**
         * Set the text of the confirm button
         */
        @CheckResult
        fun setConfirmText(text: String): Builder {
            this.confirm = text
            return this
        }

        /**
         * When the input field is required, customize the error message shown when it is empty
         */
        @CheckResult
        fun setEmptyErrorText(text: Int): Builder {
            return setEmptyErrorText(context.getString(text))
        }

        /**
         * When the input field is required, customize the error message shown when it is empty
         */
        @CheckResult
        fun setEmptyErrorText(text: String): Builder {
            this.emptyError = text
            return this
        }

        /**
         * Set whether the dialog's checkbox is enabled
         */
        @CheckResult
        fun setShowCheckBox(show: Boolean): Builder {
            this.showCheckBox = show
            return this
        }

        /**
         * Set the text of the checkbox
         */
        @CheckResult
        fun setCheckBoxText(text: Int): Builder {
            return setCheckBoxText(context.getString(text))
        }

        /**
         * Set the text of the checkbox
         */
        @CheckResult
        fun setCheckBoxText(text: String): Builder {
            this.checkBox = text
            return this
        }

        /**
         * Set the input type of the input field
         */
        @CheckResult
        fun setInputType(inputType: Int): Builder {
            this.inputType = inputType
            return this
        }

        /**
         * Set the click listener of the cancel button
         */
        @CheckResult
        fun setCancelListener(cancel: View.OnClickListener): Builder {
            this.cancelListener = cancel
            return this
        }

        /**
         * Set the click listener of the confirm button
         */
        @CheckResult
        fun setConfirmListener(confirmListener: ConfirmListener): Builder {
            this.confirmListener = confirmListener
            return this
        }

        /**
         * Show the "Set Skin" / "Set Cape" row above the confirm buttons
         */
        @CheckResult
        fun setShowSkinCape(show: Boolean): Builder {
            this.showSkinCape = show
            return this
        }

        /**
         * Set the click listener of the "Set Skin" button (requires [setShowSkinCape])
         */
        @CheckResult
        fun setSkinListener(skinListener: View.OnClickListener): Builder {
            this.skinListener = skinListener
            return this
        }

        /**
         * Set the click listener of the "Set Cape" button (requires [setShowSkinCape])
         */
        @CheckResult
        fun setCapeListener(capeListener: View.OnClickListener): Builder {
            this.capeListener = capeListener
            return this
        }

        /**
         * Mark it as required; when the user clicks confirm, check whether the input field is empty (including whitespace)
         * If it is, intercept the click event and notify the user
         */
        @CheckResult
        fun setAsRequired(): Builder {
            this.required = true
            return this
        }

        fun buildDialog(): EditTextDialog {
            return EditTextDialog(
                context,
                title, message, editText, hintText, checkBox, confirm, emptyError,
                showCheckBox, inputType,
                cancelListener, confirmListener,
                showSkinCape, skinListener, capeListener,
                required
            ).apply {
                create()
            }
        }

        fun showDialog() {
            buildDialog().show()
        }
    }
}

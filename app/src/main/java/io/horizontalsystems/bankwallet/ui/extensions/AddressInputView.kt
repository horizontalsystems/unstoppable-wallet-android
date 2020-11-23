package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import kotlinx.android.synthetic.main.view_input_address.view.*

class AddressInputView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    private var onTextChangeCallback: ((String?) -> Unit)? = null
    private var showQrButton: Boolean = false

    private val textWatcher = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onTextChangeCallback?.invoke(s?.toString())

            setDeleteButtonVisibility(!s.isNullOrBlank())
        }
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    }

    init {
        inflate(context, R.layout.view_input_address, this)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.AddressInputView)
        try {
            title.text = ta.getString(R.styleable.AddressInputView_title)
            description.text = ta.getString(R.styleable.AddressInputView_description)
            showQrButton = ta.getBoolean(R.styleable.AddressInputView_showQrButton, true)
            input.hint = ta.getString(R.styleable.AddressInputView_hint)
        } finally {
            ta.recycle()
        }

        input.addTextChangedListener(textWatcher)
        deleteButton.setOnClickListener { input.text = null }
        buttonPaste.setOnClickListener {
            input.setText(TextHelper.getCopiedText().trim())
        }
    }

    fun setText(text: String?) {
        input.apply {
            removeTextChangedListener(textWatcher)
            setText(text)
            addTextChangedListener(textWatcher)

            text?.let {
                setSelection(it.length)
            }
        }
        setDeleteButtonVisibility(!text.isNullOrBlank())
    }

    fun setHint(text: String?) {
        input.hint = text
    }

    fun setError(text: String?) {
        error.text = text
        error.isVisible = !text.isNullOrEmpty()
    }

    fun onTextChange(callback: (String?) -> Unit) {
        onTextChangeCallback = callback
    }

    fun onButtonQrScanClick(callback: () -> Unit) {
        buttonQrScan.setOnClickListener {
            callback()
        }
    }

    private fun setDeleteButtonVisibility(visible: Boolean) {
        deleteButton.isVisible = visible
        buttonQrScan.isVisible = showQrButton && !visible
        buttonPaste.isVisible = !visible
    }

}

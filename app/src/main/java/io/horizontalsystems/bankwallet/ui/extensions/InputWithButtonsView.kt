package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_input_with_buttons.view.*

class InputWithButtonsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    private var onTextChangeCallback: ((String?) -> Unit)? = null

    private val textWatcher = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onTextChangeCallback?.invoke(s?.toString())

            setDeleteButtonVisibility(!s.isNullOrBlank())
        }
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    }

    init {
        inflate(context, R.layout.view_input_with_buttons, this)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.InputWithButtonsView)
        try {
            title.text = ta.getString(R.styleable.InputWithButtonsView_title)
            description.text = ta.getString(R.styleable.InputWithButtonsView_description)
            input.inputType = ta.getInt(R.styleable.InputWithButtonsView_android_inputType, EditorInfo.TYPE_TEXT_VARIATION_NORMAL)
        } finally {
            ta.recycle()
        }

        input.addTextChangedListener(textWatcher)
        deleteButton.setOnClickListener { input.text = null }
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

    fun setLeftButtonTitle(text: String) {
        inputButtonLeft.text = text
    }

    fun onLeftButtonClick(callback: () -> Unit) {
        inputButtonLeft.setOnClickListener {
            callback()
        }
    }

    fun setRightButtonTitle(text: String) {
        inputButtonRight.text = text
    }

    fun onRightButtonClick(callback: () -> Unit) {
        inputButtonRight.setOnClickListener {
            callback()
        }
    }

    private fun setDeleteButtonVisibility(visible: Boolean) {
        deleteButton.isVisible = visible
        inputButtonLeft.isVisible = !visible
        inputButtonRight.isVisible = !visible
    }

}

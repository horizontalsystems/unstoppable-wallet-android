package io.horizontalsystems.uikit

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.view_text_input.view.*

class InputTextView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_text_input, this)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bindPrefix(prefix: String) {
        txtPrefix.text = prefix
    }

    fun getEnteredText(): String? {
        return inputEditText.text?.toString()
    }

    fun bindTextChangeListener(onTextChanged: ((String) -> Unit)) {
        inputEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val string = s.toString()
                if (string.isNotEmpty() && Character.isWhitespace(string.last())) {
                    inputEditText.setText(string.trim())
                    goToNext()
                } else {
                    onTextChanged.invoke(string.trim())
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })
    }

    fun setImeActionDone(onDone: () -> Unit) {
        inputEditText.imeOptions = EditorInfo.IME_ACTION_DONE
        inputEditText.setOnEditorActionListener { v, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    onDone.invoke()
                    true
                }
                else -> false
            }
        }
    }

    private fun goToNext() {
        inputEditText.onEditorAction(EditorInfo.IME_ACTION_NEXT)
    }
}

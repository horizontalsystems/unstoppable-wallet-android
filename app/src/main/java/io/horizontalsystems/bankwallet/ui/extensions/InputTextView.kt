package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_text_input.view.*

class InputTextView : ConstraintLayout {

    init {
        ConstraintLayout.inflate(context, R.layout.view_text_input, this)
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
        inputEditText.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable) {
                onTextChanged.invoke(s.toString().trim())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })
    }

}

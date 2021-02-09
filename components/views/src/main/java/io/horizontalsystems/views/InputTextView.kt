package io.horizontalsystems.views

import android.content.Context
import android.util.AttributeSet
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

}

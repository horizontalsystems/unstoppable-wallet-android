package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_text_input.view.*

class InputTextView : ConstraintLayout {

    constructor(context: Context) : super(context) {
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initializeViews()
    }

    private fun initializeViews() {
        ConstraintLayout.inflate(context, R.layout.view_text_input, this)
    }

    fun bindPrefix(prefix: String) {
        txtPrefix.text = prefix
    }

    fun getEnteredText(): String? {
        return inputEditText.text?.toString()
    }

}

package io.horizontalsystems.views

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.view_key_input.view.*

class InputKeyTextView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_key_input, this)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var text: String
        get() = txtKey.text.toString()
        set(value) {
            txtKey.text = value
        }

    fun bind(onCopy: () -> Unit) {
        btnCopy.setOnClickListener {
            onCopy.invoke()
        }
    }
}

package io.horizontalsystems.bankwallet.modules.send.sendviews.sendbutton

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_button_send.view.*

class SendButtonView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_button_send, this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(onSendClick: (() -> (Unit))? = null) {
        btnSend?.setOnClickListener { onSendClick?.invoke() }
    }

}

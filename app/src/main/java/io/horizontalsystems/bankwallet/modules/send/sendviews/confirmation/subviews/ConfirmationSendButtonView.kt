package io.horizontalsystems.bankwallet.modules.send.sendviews.confirmation.subviews

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_button_with_progressbar.view.*

class ConfirmationSendButtonView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_button_with_progressbar, this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(textResourceId: Int, isEnabled: Boolean = true, showProgressbar: Boolean = false) {
        buttonText.text = context.getString(textResourceId)
        buttonWrapper.isEnabled = isEnabled
        buttonText.isEnabled = isEnabled
        progressBar.visibility = if (showProgressbar) View.VISIBLE else View.GONE
        invalidate()
    }

}

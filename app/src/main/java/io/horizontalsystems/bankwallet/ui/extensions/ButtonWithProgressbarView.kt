package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_button_with_progressbar.view.*

class ButtonWithProgressbarView : ConstraintLayout {

    init {
        ConstraintLayout.inflate(context, R.layout.view_button_with_progressbar, this)
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

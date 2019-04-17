package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_amount_input.view.*

class InputAmountView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_amount_input, this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


    fun bindInitial(onMaxClick: (() -> (Unit))? = null, onSwitchClick: (() -> (Unit))? = null) {
        btnSwitch.visibility = View.VISIBLE
        btnMax.visibility =  View.VISIBLE

        btnMax?.setOnClickListener { onMaxClick?.invoke() }
        btnSwitch?.setOnClickListener { onSwitchClick?.invoke() }
        btnSwitch.imageTintMode = PorterDuff.Mode.SRC_IN
        invalidate()
    }

    fun updateInput(hint: String? = null, error: String? = null) {
        txtHintInfo.visibility = if (error == null) View.VISIBLE else View.GONE
        txtHintError.visibility = if (error == null) View.GONE else View.VISIBLE
        txtHintInfo.text = hint
        txtHintError.text = error
    }

    fun enableSwitchBtn(enabled: Boolean) {
        btnSwitch.isEnabled = enabled
    }

    fun updateAmountPrefix(prefix: String) {
        topAmountPrefix.text = prefix
    }

    fun setMaxBtnVisible(visible: Boolean) {
        btnMax.visibility = if (visible) View.VISIBLE else View.GONE
    }

}

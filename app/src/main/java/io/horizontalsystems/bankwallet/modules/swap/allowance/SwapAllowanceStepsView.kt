package io.horizontalsystems.bankwallet.modules.swap.allowance

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_swap_approve_steps.view.*

class SwapAllowanceStepsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.view_swap_approve_steps, this)
    }

    fun setStepOne() {
        stepOne.isEnabled = true
        stepTwo.isEnabled = false
        isVisible = true
    }

    fun setStepTwo() {
        stepOne.isEnabled = false
        stepTwo.isEnabled = true
        isVisible = true
    }

    fun hide() {
        isVisible = false
    }

}

package io.horizontalsystems.bankwallet.modules.swap.allowance

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.databinding.ViewSwapApproveStepsBinding

class SwapAllowanceStepsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding =
        ViewSwapApproveStepsBinding.inflate(LayoutInflater.from(context), this)

    fun setStepOne() {
        binding.stepOne.isEnabled = true
        binding.stepTwo.isEnabled = false
        isVisible = true
    }

    fun setStepTwo() {
        binding.stepOne.isEnabled = false
        binding.stepTwo.isEnabled = true
        isVisible = true
    }

    fun hide() {
        isVisible = false
    }

}

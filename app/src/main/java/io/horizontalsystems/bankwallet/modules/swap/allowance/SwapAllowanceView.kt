package io.horizontalsystems.bankwallet.modules.swap.allowance

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.LifecycleOwner
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_swap_allowance.view.*

class SwapAllowanceView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        inflate(context, R.layout.view_swap_allowance, this)
    }

    fun initialize(viewModel: SwapAllowanceViewModel, lifecycleOwner: LifecycleOwner) {
        handleVisibility(viewModel.isVisible)

        viewModel.allowanceLiveData().observe(lifecycleOwner, {
            allowance.text = it
        })

        viewModel.isVisibleLiveData().observe(lifecycleOwner, {
            handleVisibility(it)
        })

        viewModel.isErrorLiveData().observe(lifecycleOwner, { error ->
            val color = if (error) R.color.lucian else R.color.grey
            allowance.setTextColor(context.getColor(color))
        })
    }

    private fun handleVisibility(isVisible: Boolean) {
        visibility = if (isVisible) View.VISIBLE else View.GONE
    }

}

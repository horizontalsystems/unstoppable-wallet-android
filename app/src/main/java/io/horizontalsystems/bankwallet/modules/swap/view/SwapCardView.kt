package io.horizontalsystems.bankwallet.modules.swap.view

import android.content.Context
import android.util.AttributeSet
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.view_card_swap.view.*

class SwapCardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.view_card_swap, this)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.SwapCardView)
        try {
            title.text = ta.getString(R.styleable.SwapCardView_title)
        } finally {
            ta.recycle()
        }
    }

    val amountEditText: EditText
        get() = amount

    fun showEstimated(show: Boolean) {
        estimatedLabel.isVisible = show
    }

    fun showBalanceError(show: Boolean) {
        val color = if (show) {
            LayoutHelper.getAttr(R.attr.ColorLucian, context.theme, context.getColor(R.color.red_d))
        } else {
            context.getColor(R.color.grey)
        }
        balanceTitle.setTextColor(color)
        balanceValue.setTextColor(color)
    }

    fun setBalance(balance: String?) {
        balanceValue.text = balance
    }

    fun setSelectedCoin(coin: Coin?) {
        if (coin != null) {
            selectedToken.text = coin.code
            selectedToken.setTextColor(LayoutHelper.getAttr(R.attr.ColorLeah, context.theme, context.getColor(R.color.steel_light)))
        } else {
            selectedToken.text = context.getString(R.string.Swap_TokenSelectorTitle)
            selectedToken.setTextColor(LayoutHelper.getAttr(R.attr.ColorJacob, context.theme, context.getColor(R.color.yellow_d)))
        }
    }

    fun onSelectTokenButtonClick(callback: () -> Unit) {
        selectedToken.setOnClickListener {
            callback()
        }
    }

}

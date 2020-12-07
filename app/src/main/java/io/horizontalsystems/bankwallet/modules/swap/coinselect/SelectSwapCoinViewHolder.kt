package io.horizontalsystems.bankwallet.modules.swap.coinselect

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.setCoinImage
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_swap_coin_select.*

class SelectSwapCoinViewHolder(
        override val containerView: View,
        val onClick: (item: SwapCoinItem) -> Unit
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private var coinItem: SwapCoinItem? = null

    init {
        containerView.setOnSingleClickListener {
            coinItem?.let {
                onClick(it)
            }
        }
    }

    fun bind(coinItem: SwapCoinItem, showBottomBorder: Boolean) {
        this.coinItem = coinItem
        bottomShade.isVisible = showBottomBorder

        coinItem.apply {
            coinIcon.setCoinImage(coin.code, coin.type)
            coinTitle.text = coin.title
            coinSubtitle.text = coin.code
            coinBalance.text = balance?.let { App.numberFormatter.formatCoin(it, coin.code, 0, 8) }
        }
    }

}

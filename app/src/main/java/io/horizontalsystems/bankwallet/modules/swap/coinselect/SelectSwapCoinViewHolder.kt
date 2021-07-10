package io.horizontalsystems.bankwallet.modules.swap.coinselect

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.setCoinImage
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.swap.SwapModule.CoinBalanceItem
import kotlinx.android.extensions.LayoutContainer

class SelectSwapCoinViewHolder(
        override val containerView: View,
        val onClick: (item: CoinBalanceItem) -> Unit
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private var coinItem: CoinBalanceItem? = null
    private val bottomShade = containerView.findViewById<View>(R.id.bottomShade)
    private val coinIcon = containerView.findViewById<ImageView>(R.id.coinIcon)
    private val coinTitle = containerView.findViewById<TextView>(R.id.coinTitle)
    private val coinSubtitle = containerView.findViewById<TextView>(R.id.coinSubtitle)
    private val coinBalance = containerView.findViewById<TextView>(R.id.coinBalance)
    private val fiatBalance = containerView.findViewById<TextView>(R.id.fiatBalance)

    init {
        containerView.setOnSingleClickListener {
            coinItem?.let {
                onClick(it)
            }
        }
    }

    fun bind(coinItem: CoinBalanceItem, showBottomBorder: Boolean) {
        this.coinItem = coinItem
        bottomShade.isVisible = showBottomBorder

        coinItem.apply {
            coinIcon.setCoinImage(coin.type)
            coinTitle.text = coin.title
            coinSubtitle.text = coin.code

            coinBalance.text = balance?.let {
                App.numberFormatter.formatCoin(it, coin.code, 0, 8)
            }

            fiatBalance.text = fiatBalanceValue?.let {
                App.numberFormatter.formatFiat(fiatBalanceValue.value, fiatBalanceValue.currency.symbol, 0, 2)
            }
        }
    }

}

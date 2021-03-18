package io.horizontalsystems.bankwallet.modules.market.overview

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.getBackgroundTintColor
import io.horizontalsystems.bankwallet.modules.market.getText
import io.horizontalsystems.bankwallet.modules.market.getTextColor
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.views.ListPosition
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_market_overview_item.*
import java.math.BigDecimal

class ViewHolderMarketOverviewItem(override val containerView: View, private val listener: Listener)
    : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private var item: MarketViewItem? = null

    interface Listener {
        fun onItemClick(marketViewItem: MarketViewItem)
    }

    init {
        containerView.setOnClickListener {
            item?.let {
                listener.onItemClick(it)
            }
        }
    }

    fun bind(item: MarketViewItem, prev: MarketViewItem?, listPosition: ListPosition) {
        this.item = item

        if (item.coinCode != prev?.coinCode) {
            val drawableResId = AppLayoutHelper.getCoinDrawableResId(containerView.context, item.coinType)
                    ?: R.drawable.coin_placeholder
            icon.setImageResource(drawableResId)
        }

        if (item.score != prev?.score) {
            if (item.score == null) {
                rank.isVisible = false
            } else {
                item.score.apply {
                    rank.text = getText()
                    rank.setTextColor(getTextColor(containerView.context))
                    rank.backgroundTintList = ColorStateList.valueOf(getBackgroundTintColor(containerView.context))
                }
                rank.isVisible = true
            }
        }

        if (item.coinName != prev?.coinName) {
            title.text = item.coinName
        }

        if (item.coinCode != prev?.coinCode) {
            subtitle.text = item.coinCode
        }

        if (item.rate != prev?.rate) {
            rate.text = item.rate
        }

        if (item.marketDataValue != prev?.marketDataValue) {
            val marketField = item.marketDataValue

            marketFieldCaption.text = when (marketField) {
                is MarketViewItem.MarketDataValue.MarketCap -> "MCap"
                is MarketViewItem.MarketDataValue.Volume -> "Vol"
                is MarketViewItem.MarketDataValue.Diff -> ""
            }

            when (marketField) {
                is MarketViewItem.MarketDataValue.MarketCap -> {
                    marketFieldValue.text = marketField.value
                    marketFieldValue.setTextColor(containerView.resources.getColor(R.color.grey, containerView.context.theme))
                }
                is MarketViewItem.MarketDataValue.Volume -> {
                    marketFieldValue.text = marketField.value
                    marketFieldValue.setTextColor(containerView.resources.getColor(R.color.grey, containerView.context.theme))
                }
                is MarketViewItem.MarketDataValue.Diff -> {
                    val v = marketField.value
                    val sign = if (v >= BigDecimal.ZERO) "+" else "-"
                    marketFieldValue.text = App.numberFormatter.format(v.abs(), 0, 2, sign, "%")

                    val color = if (v >= BigDecimal.ZERO) R.color.remus else R.color.lucian
                    marketFieldValue.setTextColor(containerView.resources.getColor(color, containerView.context.theme))
                }
            }
        }

        rootView.setBackgroundResource(getBackground(listPosition))
    }

    private fun getBackground(listPosition: ListPosition): Int {
        return when (listPosition) {
            ListPosition.First -> R.drawable.rounded_lawrence_background_top
            ListPosition.Middle -> R.drawable.rounded_lawrence_background_middle
            ListPosition.Last -> R.drawable.rounded_lawrence_background_bottom
            ListPosition.Single -> R.drawable.rounded_lawrence_background_single
        }
    }

    companion object {
        fun create(parent: ViewGroup, listener: Listener): ViewHolderMarketOverviewItem {
            return ViewHolderMarketOverviewItem(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_market_overview_item, parent, false), listener)
        }
    }
}

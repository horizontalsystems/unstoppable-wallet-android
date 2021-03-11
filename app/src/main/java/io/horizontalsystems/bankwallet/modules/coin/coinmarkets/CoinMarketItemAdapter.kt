package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.MarketTickerViewItem
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_market_item.*

class CoinMarketItemAdapter : ListAdapter<MarketTickerViewItem, ViewHolderMarketItem>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderMarketItem {
        return ViewHolderMarketItem.create(parent)
    }

    override fun onBindViewHolder(holder: ViewHolderMarketItem, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<MarketTickerViewItem>() {
            override fun areItemsTheSame(oldItem: MarketTickerViewItem, newItem: MarketTickerViewItem): Boolean {
                return oldItem.areItemsTheSame(newItem)
            }

            override fun areContentsTheSame(oldItem: MarketTickerViewItem, newItem: MarketTickerViewItem): Boolean {
                return oldItem.areContentsTheSame(newItem)
            }
        }
    }
}

class ViewHolderMarketItem(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: MarketTickerViewItem) {

        rank.isVisible = false

        icon.setImageResource(R.drawable.coin_placeholder)

        title.text = item.title

        subtitle.text = item.subtitle

        rate.text = item.value

        marketFieldCaption.text = "Vol"

        marketFieldValue.text = item.subvalue
        marketFieldValue.setTextColor(containerView.resources.getColor(R.color.grey, containerView.context.theme))
    }

    companion object {
        fun create(parent: ViewGroup): ViewHolderMarketItem {
            return ViewHolderMarketItem(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_market_item, parent, false))
        }
    }
}

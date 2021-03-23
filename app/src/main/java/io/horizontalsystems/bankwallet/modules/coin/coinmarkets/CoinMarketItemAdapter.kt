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
import kotlinx.android.synthetic.main.view_holder_market_ticker.*

class CoinMarketItemAdapter : ListAdapter<MarketTickerViewItem, ViewHolderMarketTicker>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderMarketTicker {
        return ViewHolderMarketTicker.create(parent)
    }

    override fun onBindViewHolder(holder: ViewHolderMarketTicker, position: Int) {
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

class ViewHolderMarketTicker(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: MarketTickerViewItem) {
        title.text = item.title

        subtitle.text = item.subtitle

        rate.text = item.value

        marketFieldCaption.text = "Vol"

        marketFieldValue.text = item.subvalue
        marketFieldValue.setTextColor(containerView.resources.getColor(R.color.grey, containerView.context.theme))
    }

    companion object {
        fun create(parent: ViewGroup): ViewHolderMarketTicker {
            return ViewHolderMarketTicker(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_market_ticker, parent, false))
        }
    }
}

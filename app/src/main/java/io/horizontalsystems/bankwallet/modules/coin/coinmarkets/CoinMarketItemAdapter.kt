package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.MarketTickerViewItem
import io.horizontalsystems.bankwallet.ui.extensions.PicassoRoundedImageView
import kotlinx.android.extensions.LayoutContainer

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
        containerView.findViewById<PicassoRoundedImageView>(R.id.icon).loadImage(item.imageUrl)

        containerView.findViewById<TextView>(R.id.title).text = item.title

        containerView.findViewById<TextView>(R.id.subtitle).text = item.subtitle

        containerView.findViewById<TextView>(R.id.rate).text = item.value

        containerView.findViewById<TextView>(R.id.marketFieldCaption).text = "Vol"

        val marketFieldValue = containerView.findViewById<TextView>(R.id.marketFieldValue)
        marketFieldValue.text = item.subvalue
        marketFieldValue.setTextColor(containerView.resources.getColor(R.color.grey, containerView.context.theme))
    }

    companion object {
        fun create(parent: ViewGroup): ViewHolderMarketTicker {
            return ViewHolderMarketTicker(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_market_ticker, parent, false))
        }
    }
}

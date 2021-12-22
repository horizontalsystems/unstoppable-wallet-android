package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.databinding.ViewHolderMarketTickerBinding
import io.horizontalsystems.bankwallet.modules.coin.MarketTickerViewItem

class CoinMarketItemAdapter :
    ListAdapter<MarketTickerViewItem, ViewHolderMarketTicker>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderMarketTicker {
        return ViewHolderMarketTicker(
            ViewHolderMarketTickerBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolderMarketTicker, position: Int) = Unit

    override fun onBindViewHolder(
        holder: ViewHolderMarketTicker,
        position: Int,
        payloads: MutableList<Any>
    ) {
        holder.bind(
            getItem(position),
            payloads.firstOrNull { it is MarketTickerViewItem } as? MarketTickerViewItem)
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<MarketTickerViewItem>() {
            override fun areItemsTheSame(
                oldItem: MarketTickerViewItem,
                newItem: MarketTickerViewItem
            ): Boolean {
                return oldItem.areItemsTheSame(newItem)
            }

            override fun areContentsTheSame(
                oldItem: MarketTickerViewItem,
                newItem: MarketTickerViewItem
            ): Boolean {
                return oldItem.areContentsTheSame(newItem)
            }

            override fun getChangePayload(
                oldItem: MarketTickerViewItem,
                newItem: MarketTickerViewItem
            ): Any? {
                return oldItem
            }
        }
    }
}

class ViewHolderMarketTicker(private val binding: ViewHolderMarketTickerBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: MarketTickerViewItem, prev: MarketTickerViewItem?) {
        if (item.marketImageUrl != prev?.marketImageUrl) {
            binding.icon.loadImage(item.marketImageUrl)
        }

        if (item.market != prev?.market) {
            binding.title.text = item.market
        }

        if (item.pair != prev?.pair) {
            binding.subtitle.text = item.pair
        }

        if (item.rate != prev?.rate) {
            binding.rate.text = item.rate
        }

        if (item.volume != prev?.volume) {
            binding.marketFieldValue.text = item.volume
        }
    }

}

package io.horizontalsystems.bankwallet.modules.market.overview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.market.top.MarketTopViewItem
import io.horizontalsystems.bankwallet.modules.market.top.ViewHolderMarketTopItem

class MarketOverviewItemsAdapter(
        private val listener: ViewHolderMarketTopItem.Listener,
        private val itemsLiveData: LiveData<List<MarketTopViewItem>>,
        viewLifecycleOwner: LifecycleOwner
) : ListAdapter<MarketTopViewItem, ViewHolderMarketTopItem>(coinRateDiff) {

    init {
        itemsLiveData.observe(viewLifecycleOwner, {
            submitList(it)
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderMarketTopItem {
        return ViewHolderMarketTopItem(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin_rate, parent, false), listener)
    }

    override fun onBindViewHolder(holder: ViewHolderMarketTopItem, position: Int, payloads: MutableList<Any>) {
        holder.bind(getItem(position), payloads.firstOrNull { it is MarketTopViewItem } as? MarketTopViewItem)
    }

    override fun onBindViewHolder(holder: ViewHolderMarketTopItem, position: Int) = Unit

    companion object {
        private val coinRateDiff = object : DiffUtil.ItemCallback<MarketTopViewItem>() {
            override fun areItemsTheSame(oldItem: MarketTopViewItem, newItem: MarketTopViewItem): Boolean {
                return oldItem.areItemsTheSame(newItem)
            }

            override fun areContentsTheSame(oldItem: MarketTopViewItem, newItem: MarketTopViewItem): Boolean {
                return oldItem.areContentsTheSame(newItem)
            }

            override fun getChangePayload(oldItem: MarketTopViewItem, newItem: MarketTopViewItem): Any? {
                return oldItem
            }
        }
    }
}


package io.horizontalsystems.bankwallet.modules.market.overview

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.horizontalsystems.bankwallet.modules.market.top.MarketTopViewItem

class MarketOverviewItemsAdapter(
        private val listener: ViewHolderMarketOverviewItem.Listener,
        private val itemsLiveData: LiveData<List<MarketTopViewItem>>,
        viewLifecycleOwner: LifecycleOwner
) : ListAdapter<MarketTopViewItem, ViewHolderMarketOverviewItem>(coinRateDiff) {

    init {
        itemsLiveData.observe(viewLifecycleOwner, {
            submitList(it)
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderMarketOverviewItem {
        return ViewHolderMarketOverviewItem.create(parent, listener)
    }

    override fun onBindViewHolder(holder: ViewHolderMarketOverviewItem, position: Int, payloads: MutableList<Any>) {
        holder.bind(
                getItem(position),
                payloads.firstOrNull { it is MarketTopViewItem } as? MarketTopViewItem,
                getListPosition(position)
        )
    }

    override fun onBindViewHolder(holder: ViewHolderMarketOverviewItem, position: Int) = Unit

    private fun getListPosition(position: Int): ListPosition = when(position){
        0 -> ListPosition.First
        1 -> ListPosition.Middle
        2 -> ListPosition.Last
        else -> throw Exception("Index exceeded. This list should consist only from 3 items")
    }

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

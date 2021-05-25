package io.horizontalsystems.bankwallet.modules.coin.adapters

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.CoinDataClickType
import io.horizontalsystems.bankwallet.modules.coin.CoinDataItem
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_market_info.*

class CoinDataAdapter(
        rateDiffsLiveData: MutableLiveData<List<CoinDataItem>>,
        viewLifecycleOwner: LifecycleOwner,
        private val listener: Listener
) : ListAdapter<CoinDataItem, CoinDataAdapter.ViewHolder>(diff) {

    init {
        rateDiffsLiveData.observe(viewLifecycleOwner) {
            submitList(it)
        }
    }

    interface Listener {
        fun onClick(clickType: CoinDataClickType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(inflate(parent, R.layout.view_holder_coin_market_info, false), listener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val diff = object : DiffUtil.ItemCallback<CoinDataItem>() {
            override fun areItemsTheSame(oldItem: CoinDataItem, newItem: CoinDataItem): Boolean = true

            override fun areContentsTheSame(oldItem: CoinDataItem, newItem: CoinDataItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    class ViewHolder(override val containerView: View, private val listener: Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(item: CoinDataItem) {
            coinMarketInfoLine.bindItem(item)
            item.clickType?.let { clickType ->
                coinMarketInfoLine.setOnClickListener { listener.onClick(clickType) }
            }
        }
    }
}

package io.horizontalsystems.bankwallet.modules.coin.adapters

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
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
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        rateDiffsLiveData.observe(viewLifecycleOwner) {
            val insert = items.isEmpty()
            items = it
            if (insert) {
                notifyItemRangeInserted(0, items.size + 1)
            } else {
                notifyItemRangeChanged(0, items.size + 1)
            }
        }
    }

    private var items = listOf<CoinDataItem>()
    private val viewTypeItem = 0
    private val viewTypeSpacer = 1

    interface Listener {
        fun onClick(clickType: CoinDataClickType)
    }

    override fun getItemCount(): Int {
        return if (items.isNotEmpty()) items.size + 1 else 0
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> viewTypeSpacer
            else -> viewTypeItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            viewTypeItem -> ViewHolder(inflate(parent, R.layout.view_holder_coin_market_info, false), listener)
            viewTypeSpacer -> SpacerViewHolder(inflate(parent, R.layout.view_holder_coin_page_spacer, false))
            else -> throw  IllegalArgumentException("No such viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> holder.bind(items[position - 1])
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

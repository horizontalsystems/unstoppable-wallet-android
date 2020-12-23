package io.horizontalsystems.bankwallet.modules.market.top

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_rate.*

class MarketTopItemsAdapter(
        private val listener: Listener,
        private val viewModel: MarketTopViewModel,
        viewLifecycleOwner: LifecycleOwner
) : ListAdapter<MarketTopViewItem, ViewHolderMarketTopItem>(coinRateDiff) {

    init {
        viewModel.marketTopViewItemsLiveData.observe(viewLifecycleOwner, {
            submitList(it)
        })
    }

    interface Listener {
        fun onItemClick(marketTopViewItem: MarketTopViewItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderMarketTopItem {
        return ViewHolderMarketTopItem(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin_rate, parent, false), listener)
    }

    override fun onBindViewHolder(holder: ViewHolderMarketTopItem, position: Int) {
        holder.bind(getItem(position))
    }

    fun refresh() {
        viewModel.refresh()
    }

    companion object {
        private val coinRateDiff = object : DiffUtil.ItemCallback<MarketTopViewItem>() {
            override fun areItemsTheSame(oldItem: MarketTopViewItem, newItem: MarketTopViewItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: MarketTopViewItem, newItem: MarketTopViewItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}

class ViewHolderMarketTopItem(override val containerView: View, listener: MarketTopItemsAdapter.Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    private var item: MarketTopViewItem? = null

    init {
        containerView.setOnClickListener {
            item?.let {
                listener.onItemClick(it)
            }
        }
    }

    fun bind(item: MarketTopViewItem) {
        this.item = item

//        if (item.coin != null) {
//            coinIcon.isVisible = true
//            coinIcon.setCoinImage(item.coin.code, item.coin.type)
//        } else {
            coinIcon.isVisible = false
//        }

        if (item.rank != null) {
            rank.isVisible = true
            rank.text = item.rank.toString()
        } else {
            rank.isVisible = false
        }

        titleText.text = item.coinName
        subtitleText.text = item.coinCode

        txValueInFiat.isActivated = true
//        txValueInFiat.isActivated = !item.rateDimmed //change color via state: activated/not activated
        txValueInFiat.text = item.rate

        txDiff.isVisible = item.diff != null
        txDiffNa.isVisible = item.diff == null

        if (item.diff != null) {
            txDiff.diff = item.diff
        }
    }
}

package io.horizontalsystems.bankwallet.modules.market.top

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_rate.*

class MarketTopItemsAdapter(
        private val listener: ViewHolderMarketTopItem.Listener,
        private val itemsLiveData: LiveData<List<MarketTopViewItem>>,
        private val loadingLiveData: LiveData<Boolean>,
        private val errorLiveData: LiveData<String?>,
        viewLifecycleOwner: LifecycleOwner
) : ListAdapter<MarketTopViewItem, ViewHolderMarketTopItem>(coinRateDiff) {

    init {
        itemsLiveData.observe(viewLifecycleOwner, {
            submitList(it)
        })
        errorLiveData.observe(viewLifecycleOwner, { error ->
            if (error != null) {
                submitList(listOf())
            }
        })
        loadingLiveData.observe(viewLifecycleOwner, { loading ->
            if (loading) {
                submitList(listOf())
            }
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

class ViewHolderMarketTopItem(override val containerView: View, private val listener: Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    private var item: MarketTopViewItem? = null

    interface Listener {
        fun onItemClick(marketTopViewItem: MarketTopViewItem)
    }

    init {
        containerView.setOnClickListener {
            item?.let {
                listener.onItemClick(it)
            }
        }
    }

    fun bind(item: MarketTopViewItem, prev: MarketTopViewItem?) {
        this.item = item

//        if (item.coin != null) {
//            coinIcon.isVisible = true
//            coinIcon.setCoinImage(item.coin.code, item.coin.type)
//        } else {
        coinIcon.isVisible = false
//        }

        if (item.rank != prev?.rank) {
            if (item.rank != null) {
                rank.isVisible = true
                rank.text = item.rank.toString()
            } else {
                rank.isVisible = false
            }
        }

        if (item.coinName != prev?.coinName) {
            titleText.text = item.coinName
        }

        if (item.coinCode != prev?.coinCode) {
            subtitleText.text = item.coinCode
        }


        if (item.rate != prev?.rate) {
            txValueInFiat.isActivated = true
//        txValueInFiat.isActivated = !item.rateDimmed //change color via state: activated/not activated
            txValueInFiat.text = item.rate
        }

        if (item.diff != prev?.diff) {
            txDiff.isVisible = item.diff != null
            txDiffNa.isVisible = item.diff == null

            if (item.diff != null) {
                txDiff.diff = item.diff
            }
        }
    }
}

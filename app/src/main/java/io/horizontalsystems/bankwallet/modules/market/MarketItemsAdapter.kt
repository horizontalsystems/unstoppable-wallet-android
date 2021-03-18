package io.horizontalsystems.bankwallet.modules.market

import android.content.res.ColorStateList
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
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_market_item.*
import java.math.BigDecimal

class MarketItemsAdapter(
        private val listener: ViewHolderMarketItem.Listener,
        itemsLiveData: LiveData<Pair<List<MarketViewItem>, Boolean>>,
        loadingLiveData: LiveData<Boolean>,
        errorLiveData: LiveData<String?>,
        viewLifecycleOwner: LifecycleOwner
) : ListAdapter<MarketViewItem, ViewHolderMarketItem>(coinRateDiff) {

    init {
        itemsLiveData.observe(viewLifecycleOwner, { (list, scrollToTop) ->
            submitList(list) {
                if (scrollToTop)
                    recyclerView.scrollToPosition(0)
            }
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

    private lateinit var recyclerView: RecyclerView

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderMarketItem {
        return ViewHolderMarketItem.create(parent, listener)
    }

    override fun onBindViewHolder(holder: ViewHolderMarketItem, position: Int, payloads: MutableList<Any>) {
        holder.bind(getItem(position), payloads.firstOrNull { it is MarketViewItem } as? MarketViewItem)
    }

    override fun onBindViewHolder(holder: ViewHolderMarketItem, position: Int) = Unit

    companion object {
        private val coinRateDiff = object : DiffUtil.ItemCallback<MarketViewItem>() {
            override fun areItemsTheSame(oldItem: MarketViewItem, newItem: MarketViewItem): Boolean {
                return oldItem.areItemsTheSame(newItem)
            }

            override fun areContentsTheSame(oldItem: MarketViewItem, newItem: MarketViewItem): Boolean {
                return oldItem.areContentsTheSame(newItem)
            }

            override fun getChangePayload(oldItem: MarketViewItem, newItem: MarketViewItem): Any? {
                return oldItem
            }
        }
    }
}

class ViewHolderMarketItem(override val containerView: View, private val listener: Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    private var item: MarketViewItem? = null

    interface Listener {
        fun onItemClick(marketViewItem: MarketViewItem)
    }

    init {
        containerView.setOnClickListener {
            item?.let {
                listener.onItemClick(it)
            }
        }
    }

    fun bind(item: MarketViewItem, prev: MarketViewItem?) {
        this.item = item

        if (item.coinCode != prev?.coinCode) {
            val drawableResId = AppLayoutHelper.getCoinDrawableResId(containerView.context, item.coinType)
                    ?: R.drawable.coin_placeholder
            icon.setImageResource(drawableResId)
        }

        if (prev == null || item.score != prev.score) {
            if (item.score == null) {
                rank.isVisible = false
            } else {
                item.score.apply {
                    rank.text = getText()
                    rank.setTextColor(getTextColor(containerView.context))
                    rank.backgroundTintList = ColorStateList.valueOf(getBackgroundTintColor(containerView.context))
                }
                rank.isVisible = true
            }
        }

        if (item.coinName != prev?.coinName) {
            title.text = item.coinName
        }

        if (item.coinCode != prev?.coinCode) {
            subtitle.text = item.coinCode
        }

        if (item.rate != prev?.rate) {
            rate.text = item.rate
        }

        if (item.marketDataValue != prev?.marketDataValue) {
            val marketField = item.marketDataValue

            marketFieldCaption.text = when (marketField) {
                is MarketViewItem.MarketDataValue.MarketCap -> "MCap"
                is MarketViewItem.MarketDataValue.Volume -> "Vol"
                is MarketViewItem.MarketDataValue.Diff -> ""
            }

            when (marketField) {
                is MarketViewItem.MarketDataValue.MarketCap -> {
                    marketFieldValue.text = marketField.value
                    marketFieldValue.setTextColor(containerView.resources.getColor(R.color.grey, containerView.context.theme))
                }
                is MarketViewItem.MarketDataValue.Volume -> {
                    marketFieldValue.text = marketField.value
                    marketFieldValue.setTextColor(containerView.resources.getColor(R.color.grey, containerView.context.theme))
                }
                is MarketViewItem.MarketDataValue.Diff -> {
                    val v = marketField.value
                    val sign = if (v >= BigDecimal.ZERO) "+" else "-"
                    marketFieldValue.text = App.numberFormatter.format(v.abs(), 0, 2, sign, "%")

                    val color = if (v >= BigDecimal.ZERO) R.color.remus else R.color.lucian
                    marketFieldValue.setTextColor(containerView.context.getColor(color))
                }
            }
        }

    }

    companion object {
        fun create(parent: ViewGroup, listener: Listener): ViewHolderMarketItem {
            return ViewHolderMarketItem(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_market_item, parent, false), listener)
        }
    }
}

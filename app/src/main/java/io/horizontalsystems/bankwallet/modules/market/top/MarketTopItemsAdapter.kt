package io.horizontalsystems.bankwallet.modules.market.top

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_market_item.*
import java.math.BigDecimal

class MarketTopItemsAdapter(
        private val listener: ViewHolderMarketTopItem.Listener,
        itemsLiveData: LiveData<List<MarketTopViewItem>>,
        loadingLiveData: LiveData<Boolean>,
        errorLiveData: LiveData<String?>,
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
        return ViewHolderMarketTopItem.create(parent, listener)
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

        if (item.coinCode != prev?.coinCode) {
            val drawableResId = AppLayoutHelper.getCoinDrawableResId(containerView.context, item.coinCode)
                    ?: R.drawable.coin_placeholder
            icon.setImageResource(drawableResId)
        }

        if (item.score != prev?.score) {
            item.score.apply {
                rank.text = getText()
                rank.setTextColor(getTextColor(containerView.context))
                rank.backgroundTintList = ColorStateList.valueOf(getBackgroundTintColor(containerView.context))
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
                is MarketTopViewItem.MarketDataValue.MarketCap -> "MCap"
                is MarketTopViewItem.MarketDataValue.Volume -> "Vol"
                is MarketTopViewItem.MarketDataValue.Diff -> ""
            }

            when (marketField) {
                is MarketTopViewItem.MarketDataValue.MarketCap -> {
                    marketFieldValue.text = marketField.value
                    marketFieldValue.setTextColor(containerView.resources.getColor(R.color.grey, containerView.context.theme))
                }
                is MarketTopViewItem.MarketDataValue.Volume -> {
                    marketFieldValue.text = marketField.value
                    marketFieldValue.setTextColor(containerView.resources.getColor(R.color.grey, containerView.context.theme))
                }
                is MarketTopViewItem.MarketDataValue.Diff -> {
                    val v = marketField.value
                    val sign = if (v >= BigDecimal.ZERO) "+" else "-"
                    marketFieldValue.text = App.numberFormatter.format(v.abs(), 0, 2, sign, "%")

                    val textColor = if (v >= BigDecimal.ZERO) R.attr.ColorRemus else R.attr.ColorLucian
                    LayoutHelper.getAttr(textColor, containerView.context.theme)?.let {
                        marketFieldValue.setTextColor(it)
                    }
                }
            }
        }

    }

    companion object {
        fun create(parent: ViewGroup, listener: Listener): ViewHolderMarketTopItem {
            return ViewHolderMarketTopItem(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_market_item, parent, false), listener)
        }
    }
}

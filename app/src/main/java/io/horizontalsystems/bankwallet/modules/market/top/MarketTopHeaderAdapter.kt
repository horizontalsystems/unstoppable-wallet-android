package io.horizontalsystems.bankwallet.modules.market.top

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_sorting.*
import java.lang.IllegalStateException

class MarketTopHeaderAdapter(
        private val listener: Listener,
        sortingField: SortingField,
        marketField: MarketField
) : RecyclerView.Adapter<MarketTopHeaderAdapter.ViewHolderHeader>() {

    var sortingField = sortingField
        private set

    var marketField = marketField
        private set

    fun update(sortingField: SortingField? = null, marketField: MarketField? = null) {
        sortingField?.let {
            this.sortingField = it
        }
        marketField?.let {
            this.marketField = it
        }
        notifyItemChanged(0)
    }

    interface Listener {
        fun onClickSortingField()
        fun onSelectMarketField(marketField: MarketField)
    }

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderHeader {
        return ViewHolderHeader(inflate(parent, R.layout.view_holder_sorting, false), listener)
    }

    override fun onBindViewHolder(holder: ViewHolderHeader, position: Int) {
        holder.bind(sortingField, marketField)
    }

    class ViewHolderHeader(override val containerView: View, private val listener: Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        private var triggerMarketFieldChangeListener = true

        init {
            sortingField.setOnSingleClickListener {
                listener.onClickSortingField()
            }
            marketFieldSelector.setOnCheckedChangeListener { group, checkedId ->
                if (!triggerMarketFieldChangeListener) return@setOnCheckedChangeListener

                val marketField = when (checkedId) {
                    R.id.fieldMarketCap -> MarketField.MarketCap
                    R.id.fieldVolume -> MarketField.Volume
                    R.id.fieldPriceDiff -> MarketField.PriceDiff
                    else -> throw IllegalStateException("")
                }

                listener.onSelectMarketField(marketField)
            }
        }

        fun bind(fieldToSort: SortingField, marketField: MarketField) {
            val sortingFieldText = containerView.context.getString(fieldToSort.titleResId)
            if (sortingField.text != sortingFieldText) {
                sortingField.text = sortingFieldText
            }

            val selectedMarketFieldId = when (marketField) {
                MarketField.MarketCap -> R.id.fieldMarketCap
                MarketField.Volume -> R.id.fieldVolume
                MarketField.PriceDiff -> R.id.fieldPriceDiff
            }
            triggerMarketFieldChangeListener = false
            marketFieldSelector.check(selectedMarketFieldId)
            triggerMarketFieldChangeListener = true
        }
    }

}

package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.SortingField
import kotlinx.android.synthetic.main.view_market_list_header.view.*

class MarketListHeaderView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onClickSortingField()
        fun onSelectMarketField(marketField: MarketField)
    }

    var listener: Listener? = null

    private var triggerMarketFieldChangeListener = true

    init {
        inflate(context, R.layout.view_market_list_header, this)

        sortingField.setOnSingleClickListener {
            listener?.onClickSortingField()
        }
        marketFieldSelector.setOnCheckedChangeListener { _, checkedId ->
            if (!triggerMarketFieldChangeListener) return@setOnCheckedChangeListener

            val marketField = when (checkedId) {
                R.id.fieldMarketCap -> MarketField.MarketCap
                R.id.fieldVolume -> MarketField.Volume
                R.id.fieldPriceDiff -> MarketField.PriceDiff
                else -> throw IllegalStateException("")
            }

            listener?.onSelectMarketField(marketField)
        }
    }

    fun setSortingField(fieldToSort: SortingField) {
        val sortingFieldText = context.getString(fieldToSort.titleResId)
        if (sortingField.text != sortingFieldText) {
            sortingField.text = sortingFieldText
        }
    }

    fun setMarketField(marketField: MarketField) {
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

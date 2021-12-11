package io.horizontalsystems.bankwallet.modules.transactionInfo

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.CoinDataItem
import io.horizontalsystems.views.ListPosition
import kotlinx.android.synthetic.main.view_coin_info_item.view.*

class CoinInfoItemView : ConstraintLayout {
    init {
        inflate(context, R.layout.view_coin_info_item, this)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(
            title: String,
            value: String? = null,
            valueLabeled: String? = null,
            icon: Int? = null,
            rank: String? = null,
            listPosition: ListPosition
    ) {
        txtTitle.text = title

        txtRank.isVisible = rank != null
        txtRank.text = rank

        if (value != null) {
            valueText.isVisible = true
            valueText.text = value
        } else if (valueLabeled != null) {
            labeledText.isVisible = true
            labeledText.text = valueLabeled
        }

        iconView.isVisible = icon != null

        icon?.let {
            iconView.setImageResource(it)
        }

        viewBackground.setBackgroundResource(listPosition.getBackground())

        invalidate()
    }

    fun bindItem(item: CoinDataItem) {
        bind(
            title = item.title,
            value = item.value,
            valueLabeled = item.valueLabeled,
            listPosition = item.listPosition ?: ListPosition.Middle,
            icon = item.icon,
            rank = item.rankLabel
        )

        item.valueLabeledBackground?.let { color ->
            labeledText.setBackgroundResource(color)
        }
    }
}

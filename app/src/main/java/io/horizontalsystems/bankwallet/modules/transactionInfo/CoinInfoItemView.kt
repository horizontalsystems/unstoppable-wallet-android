package io.horizontalsystems.bankwallet.modules.transactionInfo

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.databinding.ViewCoinInfoItemBinding
import io.horizontalsystems.bankwallet.modules.coin.CoinDataItem
import io.horizontalsystems.views.ListPosition

class CoinInfoItemView : ConstraintLayout {
    private val binding = ViewCoinInfoItemBinding.inflate(LayoutInflater.from(context), this)

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun bind(
        title: String,
        value: String? = null,
        valueLabeled: String? = null,
        icon: Int? = null,
        rank: String? = null,
        listPosition: ListPosition
    ) {
        binding.txtTitle.text = title

        binding.txtRank.isVisible = rank != null
        binding.txtRank.text = rank

        if (value != null) {
            binding.valueText.isVisible = true
            binding.valueText.text = value
        } else if (valueLabeled != null) {
            binding.labeledText.isVisible = true
            binding.labeledText.text = valueLabeled
        }

        binding.iconView.isVisible = icon != null

        icon?.let {
            binding.iconView.setImageResource(it)
        }

        binding.viewBackground.setBackgroundResource(listPosition.getBackground())

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
            binding.labeledText.setBackgroundResource(color)
        }
    }
}

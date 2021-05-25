package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.coin.CoinDataItem
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
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
            icon: Int? = null,
            decoratedValue: String? = null,
            listPosition: ListPosition
    ) {
        txtTitle.text = title
        decoratedText.isVisible = !decoratedValue.isNullOrBlank()

        decoratedValue?.let {
            decoratedText.text = decoratedValue
            decoratedText.setOnSingleClickListener {
                TextHelper.copyText(decoratedValue)
                HudHelper.showSuccessMessage(this, R.string.Hud_Text_Copied)
            }
        }

        valueText.text = value
        valueText.isVisible = !value.isNullOrBlank()

        iconView.isVisible = icon != null

        icon?.let {
            iconView.setImageResource(it)
        }

        viewBackground.setBackgroundResource(listPosition.getBackground())

        invalidate()
    }

    fun bindItem(item: CoinDataItem){
        bind(
                title = context.getString(item.title),
                value = item.value,
                listPosition = item.listPosition ?: ListPosition.Middle,
                icon = item.icon
        )
    }

}

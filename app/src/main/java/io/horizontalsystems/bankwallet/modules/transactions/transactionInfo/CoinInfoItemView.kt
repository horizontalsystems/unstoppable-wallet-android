package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.ListPosition
import kotlinx.android.synthetic.main.view_coin_info_item.view.*

class CoinInfoItemView : ConstraintLayout {
    init {
        inflate(context, R.layout.view_coin_info_item, this)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(title: String, value: String? = null, decoratedValue: String? = null, listPosition: ListPosition) {
        txtTitle.text = title
        decoratedText.text = decoratedValue
        decoratedText.isVisible = !decoratedValue.isNullOrBlank()

        valueText.text = value
        valueText.isVisible = !value.isNullOrBlank()

        viewBackground.setBackgroundResource(listPosition.getBackground())

        invalidate()
    }

}

package io.horizontalsystems.bankwallet.modules.transactionInfo

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.ListPosition
import kotlinx.android.synthetic.main.view_transaction_info_item.view.*

class TransactionInfoItemView : ConstraintLayout {
    init {
        inflate(context, R.layout.view_transaction_info_item, this)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(title: String, address: String, listPosition: ListPosition, onHashClick: () -> Unit) {
        txtTitle.text = title
        decoratedText.text = address
        txViewBackground.setBackgroundResource(listPosition.getBackground())

        decoratedText.setOnClickListener {
            onHashClick.invoke()
        }

        invalidate()
    }

}

package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_transaction_info_item.view.*

class TransactionInfoItemView : ConstraintLayout {
    init {
        inflate(context, R.layout.view_transaction_info_item, this)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bindHashId(title: String, address: String) {
        txtTitle.text = title
        decoratedText.text = address
        decoratedText.visibility = View.VISIBLE

        btnAction.visibility = View.GONE
        transactionStatusView.visibility = View.GONE

        invalidate()
    }

}

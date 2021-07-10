package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R

class TransactionInfoItemView : ConstraintLayout {

    private var txtTitle: TextView
    private var decoratedText: TextView
    private var btnAction: ImageButton
    private var transactionStatusView: TransactionInfoStatusView

    init {
        val rootView = inflate(context, R.layout.view_transaction_info_item, this)
        txtTitle = rootView.findViewById(R.id.txtTitle)
        decoratedText = rootView.findViewById(R.id.decoratedText)
        btnAction = rootView.findViewById(R.id.btnAction)
        transactionStatusView = rootView.findViewById(R.id.transactionStatusView)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bindHashId(title: String, address: String) {
        txtTitle.text = title
        decoratedText.text = address
        decoratedText.isVisible = true

        btnAction.isVisible = false
        transactionStatusView.isVisible = false

        invalidate()
    }

}

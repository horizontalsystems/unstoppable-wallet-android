package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import kotlinx.android.synthetic.main.view_transaction_info_item.view.*

class TransactionInfoItemView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_transaction_info_item, this)
    }

    private var attrTitle: String? = null
    private var attrValue: String? = null
    private var attrValueSubtitle: String? = null
    private var attrShowValueBackground: String? = null
    private var attrValueIcon: String? = null


    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { loadAttributes(attrs) }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { loadAttributes(attrs) }

    fun bind(title: String? = null, value: String? = null) {
        txtTitle.text = title
        valueText.text = value
        valueText.visibility = if (value == null) View.GONE else View.VISIBLE
        border.visibility = View.VISIBLE
    }

    fun bindAddress(title: String? = null, address: String? = null, showBottomBorder: Boolean = false) {
        txtTitle.text = title
        address?.let { addressView.bind(it) }
        addressView.visibility = if (address == null) View.GONE else View.VISIBLE
        border.visibility = if (showBottomBorder) View.VISIBLE else View.GONE

        invalidate()
    }

    fun bindStatus(transactionStatus: TransactionStatus) {
        transactionStatusView.visibility = View.VISIBLE
        border.visibility = View.VISIBLE
        txtTitle.setText(R.string.TransactionInfo_Status)
        transactionStatusView.bind(transactionStatus)
        invalidate()
    }

    private fun loadAttributes(attrs: AttributeSet) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.TransactionInfoItemView, 0, 0)
        try {
            attrTitle = ta.getString(R.styleable.TransactionInfoItemView_title)
            attrValue = ta.getString(R.styleable.TransactionInfoItemView_value)
            attrValueSubtitle = ta.getString(R.styleable.TransactionInfoItemView_valueSubtitle)
            attrShowValueBackground = ta.getString(R.styleable.TransactionInfoItemView_showValueBackground)
            attrValueIcon = ta.getString(R.styleable.TransactionInfoItemView_valueIcon)
        } finally {
            ta.recycle()
        }
    }

}

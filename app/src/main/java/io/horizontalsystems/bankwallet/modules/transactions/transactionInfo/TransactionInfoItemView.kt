package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.views.LayoutHelper
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

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        loadAttributes(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        loadAttributes(attrs)
    }

    fun bind(title: String, value: String) {
        txtTitle.text = title
        valueText.text = value
        valueText.visibility = View.VISIBLE

        invalidate()
    }

    fun bindAddress(title: String, address: String) {
        txtTitle.text = title
        decoratedText.text = address
        decoratedText.visibility = View.VISIBLE

        invalidate()
    }

    fun bindStatus(transactionStatus: TransactionStatus, incoming: Boolean) {
        txtTitle.setText(R.string.TransactionInfo_Status)
        transactionStatusView.bind(transactionStatus, incoming)
        transactionStatusView.visibility = View.VISIBLE

        invalidate()
    }

    fun bindHashId(title: String, address: String) {
        txtTitle.text = title
        decoratedText.text = address
        decoratedText.visibility = View.VISIBLE
        decoratedText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.hash, 0, 0, 0)

        invalidate()
    }

    fun bindInfo(info: String, @DrawableRes infoIcon: Int) {
        txtTitle.text = info
        txtTitle.visibility = View.VISIBLE
        txtTitle.setCompoundDrawablesWithIntrinsicBounds(infoIcon, 0, 0, 0)
        txtTitle.compoundDrawablePadding = LayoutHelper.dp(11f, context)

        valueText.text = null
        valueText.visibility = View.VISIBLE
        valueText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_info, 0)
        valueText.compoundDrawablePadding = LayoutHelper.dp(16f, context)

        invalidate()
    }

    fun bindSentToSelfNote() {
        txtTitle.setText(R.string.TransactionInfo_SentToSelfNote)
        txtTitle.visibility = View.VISIBLE
        txtTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_incoming_16, 0, 0, 0)
        txtTitle.compoundDrawablePadding = LayoutHelper.dp(11f, context)

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

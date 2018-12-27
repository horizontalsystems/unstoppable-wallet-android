package io.horizontalsystems.bankwallet.modules.transactionInfo

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import kotlinx.android.synthetic.main.view_transaction_info_item.view.*

class TransactionInfoItemView : ConstraintLayout {

    private var attrTitle: String? = null
    private var attrValue: String? = null
    private var attrValueSubtitle: String? = null
    private var attrShowValueBackground: String? = null
    private var attrValueIcon: String? = null


    constructor(context: Context) : super(context) {
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initializeViews()
        loadAttributes(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initializeViews()
        loadAttributes(attrs)
    }

    private fun initializeViews() {
        ConstraintLayout.inflate(context, R.layout.view_transaction_info_item, this)
    }

    fun bind(title: String? = null, valueTitle: String? = null, valueSubtitle: String? = null, valueIcon: Int? = null, showBottomBorder: Boolean = false) {
        valueWrapper.visibility = View.VISIBLE

        txtTitle.text = title
        txtValueTitle.text = valueTitle
        txtValueSubtitle.text = valueSubtitle
        valueIcon?.let {
            valueLeftIcon.setImageDrawable(ContextCompat.getDrawable(App.instance, it))
            valueLeftIcon.visibility = View.VISIBLE
        } ?: run {
            valueLeftIcon.visibility = View.GONE
            valueWrapper.background = null
        }

        txtValueSubtitle.visibility = if (valueSubtitle == null) View.GONE else View.VISIBLE
        border.visibility = if (showBottomBorder) View.VISIBLE else View.GONE

        invalidate()
    }

    fun bindStatus(transactionStatus: TransactionStatus) {
        valueWrapper.visibility = View.GONE
        border.visibility = View.VISIBLE
        txtTitle.setText(R.string.TransactionInfo_Status)

        when (transactionStatus) {
            is TransactionStatus.Pending -> {
                pendingIcon.visibility = View.VISIBLE
                statusProgressBar.visibility = View.GONE
            }
            is TransactionStatus.Processing -> {
                pendingIcon.visibility = View.GONE
                statusProgressBar.visibility = View.VISIBLE
                setProgressBars(transactionStatus.progress)
            }
            else -> {
                pendingIcon.visibility = View.GONE
                statusProgressBar.visibility = View.VISIBLE
                setProgressBars()
            }
        }

        invalidate()
    }

    private fun setProgressBars(progress: Int = 6) {
        if (progress == 0) return
        val greenBar = R.drawable.status_progress_bar_green

        progressBar1.setImageResource(greenBar)
        if (progress == 1) return
        progressBar2.setImageResource(greenBar)
        if (progress == 2) return
        progressBar3.setImageResource(greenBar)
        if (progress == 3) return
        progressBar4.setImageResource(greenBar)
        if (progress == 4) return
        progressBar5.setImageResource(greenBar)
        if (progress == 5) return
        progressBar6.setImageResource(greenBar)
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

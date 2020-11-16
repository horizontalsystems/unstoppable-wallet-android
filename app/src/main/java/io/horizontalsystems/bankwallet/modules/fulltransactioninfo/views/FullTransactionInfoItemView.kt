package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.views

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.FullTransactionIcon
import kotlinx.android.synthetic.main.view_transaction_full_info_item.view.*

class FullTransactionInfoItemView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_transaction_full_info_item, this)
    }

    private var attrTitle: String? = null
    private var attrValue: String? = null
    private var attrValueIcon: String? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { loadAttributes(attrs) }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { loadAttributes(attrs) }

    fun bind(title: String? = null, value: String? = null, icon: FullTransactionIcon?, dimmed: Boolean = false, bottomBorder: Boolean = false) {
        txtTitle.text = title

        var isAddress = false

        when (icon) {
            FullTransactionIcon.PERSON -> value?.let {
                isAddress = true
                addressView.text = it
            }
            FullTransactionIcon.TOKEN -> value?.let {
                isAddress = true
                addressView.text = it
                addressView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.token, 0, 0, 0)
            }

            FullTransactionIcon.TIME -> showTypeIcon(R.drawable.pending_grey)
            FullTransactionIcon.BLOCK -> showTypeIcon(R.drawable.blocks)
            FullTransactionIcon.CHECK -> showTypeIcon(R.drawable.ic_checkmark)
        }

        valueText.isVisible = !isAddress
        addressView.isVisible = isAddress

        if (!isAddress){
            valueText.text = value
        }

        if (dimmed) {
            valueText.setTextColor(ContextCompat.getColor(valueText.context, R.color.grey))
        }

        border.isVisible = bottomBorder
        invalidate()
    }

    fun bindSourceProvider(title: String, value: String?) {
        txtTitle.text = title
        sourceProviderText.text = value

        sourceProviderText.isVisible = true
        rightArrow.isVisible = true
        valueText.isVisible = false
        addressView.isVisible = false
    }

    private fun showTypeIcon(icon: Int) {
        typeIcon.setImageDrawable(ContextCompat.getDrawable(context, icon))
        typeIcon.isVisible = true
    }

    private fun loadAttributes(attrs: AttributeSet) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.FullTransactionInfoItemView, 0, 0)
        try {
            attrTitle = ta.getString(R.styleable.FullTransactionInfoItemView_infoTitle)
            attrValue = ta.getString(R.styleable.FullTransactionInfoItemView_infoValue)
            attrValueIcon = ta.getString(R.styleable.FullTransactionInfoItemView_infoValueIcon)
        } finally {
            ta.recycle()
        }
    }

}

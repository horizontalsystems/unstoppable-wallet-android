package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.ListPosition

class CoinInfoItemView : ConstraintLayout {

    private var txtTitle: TextView
    private var decoratedText: TextView
    private var valueText: TextView
    private var iconView: ImageView
    private var viewBackground: View

    init {
        val rootView = inflate(context, R.layout.view_coin_info_item, this)
        txtTitle = rootView.findViewById(R.id.txtTitle)
        decoratedText = rootView.findViewById(R.id.decoratedText)
        valueText = rootView.findViewById(R.id.valueText)
        iconView = rootView.findViewById(R.id.iconView)
        viewBackground = rootView.findViewById(R.id.viewBackground)
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

}

package io.horizontalsystems.bankwallet.modules.send.sendviews.confirmation.subviews

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.send.sendviews.confirmation.SendConfirmationModule
import kotlinx.android.synthetic.main.view_confirmation_primary_item_view.view.*

class ConfirmationPrimaryView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_confirmation_primary_item_view, this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(primaryItemData: SendConfirmationModule.PrimaryItemData, onReceiverClick: (() -> (Unit))) {
        primaryAmountText.text = primaryItemData.primaryAmount
        secondaryAmountText.text = primaryItemData.secondaryAmount
        receiverView.bind(primaryItemData.receiver)
        receiverView.setOnClickListener { onReceiverClick.invoke() }
    }

}

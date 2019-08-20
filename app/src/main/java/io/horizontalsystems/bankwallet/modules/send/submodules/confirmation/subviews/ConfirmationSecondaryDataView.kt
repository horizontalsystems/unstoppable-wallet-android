package io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.subviews

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.SendConfirmationModule
import kotlinx.android.synthetic.main.view_confirmation_secondary_data_item_view.view.*

class ConfirmationSecondaryDataView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_confirmation_secondary_data_item_view, this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(secondaryData: SendConfirmationModule.SecondaryItemData) {
        secondaryData.feeAmount?.let {
            feeWrapper.visibility = View.VISIBLE
            feeValue.text = it
        }
        secondaryData.totalAmount?.let {
            totalWrapper.visibility = View.VISIBLE
            totalValue.text = it
        }
        secondaryData.estimatedTime?.let {
            timeWrapper.visibility = View.VISIBLE
            timeValue.text = it
        }
    }

}

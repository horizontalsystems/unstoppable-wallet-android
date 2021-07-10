package io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.subviews

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stringResId
import io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.SendConfirmationModule
import io.horizontalsystems.core.helpers.DateHelper

class ConfirmationSecondaryView : ConstraintLayout {

    private var feeLayout: LinearLayout
    private var feeValue: TextView
    private var timeWrapper: LinearLayout
    private var timeValue: TextView
    private var lockTimeWrapper: LinearLayout
    private var textLockTime: TextView

    init {
        val rootView = inflate(context, R.layout.view_confirmation_secondary_item_view, this)
        feeLayout = rootView.findViewById(R.id.feeLayout)
        feeValue = rootView.findViewById(R.id.feeValue)
        timeWrapper = rootView.findViewById(R.id.timeWrapper)
        timeValue = rootView.findViewById(R.id.timeValue)
        lockTimeWrapper = rootView.findViewById(R.id.lockTimeWrapper)
        textLockTime = rootView.findViewById(R.id.textLockTime)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(secondaryData: SendConfirmationModule.SecondaryItemData) {
        secondaryData.feeAmount?.let {
            feeLayout.isVisible = true
            feeValue.text = it
        }
        secondaryData.estimatedTime?.let {
            timeWrapper.isVisible = true
            timeValue.text = DateHelper.getTxDurationIntervalString(context, it)
        }
        secondaryData.lockTimeInterval?.let {
            lockTimeWrapper.isVisible = true
            textLockTime.setText(it.stringResId())
        }
    }

}

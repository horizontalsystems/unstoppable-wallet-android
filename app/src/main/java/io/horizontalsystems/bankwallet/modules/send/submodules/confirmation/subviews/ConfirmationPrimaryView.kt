package io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.subviews

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.SendConfirmationModule

class ConfirmationPrimaryView : ConstraintLayout {

    private var primaryName: TextView
    private var primaryAmount: TextView
    private var secondaryName: TextView
    private var secondaryAmount: TextView
    private var domainValue: TextView
    private var domainTitle: TextView
    private var borderDomain: TextView
    private var receiverValue: TextView
    private var borderMemo: View
    private var memoLayout: ConstraintLayout
    private var memoValue: TextView

    init {
        val rootView = inflate(context, R.layout.view_confirmation_primary_item_view, this)
        primaryName = rootView.findViewById(R.id.primaryName)
        primaryAmount = rootView.findViewById(R.id.primaryAmount)
        secondaryName = rootView.findViewById(R.id.secondaryName)
        secondaryAmount = rootView.findViewById(R.id.secondaryAmount)
        domainValue = rootView.findViewById(R.id.domainValue)
        domainTitle = rootView.findViewById(R.id.domainTitle)
        borderDomain = rootView.findViewById(R.id.borderDomain)
        receiverValue = rootView.findViewById(R.id.receiverValue)
        borderMemo = rootView.findViewById(R.id.borderMemo)
        memoLayout = rootView.findViewById(R.id.memoLayout)
        memoValue = rootView.findViewById(R.id.memoValue)
    }


    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(primaryItemData: SendConfirmationModule.PrimaryItemData, onReceiverClick: (() -> (Unit))) {
        primaryName.text = primaryItemData.primaryName
        primaryAmount.text = primaryItemData.primaryAmount
        primaryAmount.setCompoundDrawablesWithIntrinsicBounds(0, 0, if (primaryItemData.locked) R.drawable.ic_lock_20 else 0, 0)
        secondaryName.text = primaryItemData.secondaryName
        secondaryAmount.text = primaryItemData.secondaryAmount

        primaryItemData.domain?.let {
            domainValue.text = it
            domainValue.isVisible = true
            domainTitle.isVisible = true
            borderDomain.isVisible = true
        }

        receiverValue.text = primaryItemData.receiver
        receiverValue.setOnClickListener { onReceiverClick.invoke() }

        primaryItemData.memo?.let {
            borderMemo.isVisible = true
            memoLayout.isVisible = true
            memoValue.text = it
        }
    }
}

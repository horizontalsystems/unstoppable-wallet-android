package io.horizontalsystems.bankwallet.modules.transactions

import android.content.Context
import android.graphics.drawable.Animatable
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_transaction_pending_status.view.*


class TransactionPendingStatusView : ConstraintLayout {

    constructor(context: Context) : super(context) {
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initializeViews()
    }

    private fun initializeViews() {
        inflate(context, R.layout.view_transaction_pending_status, this)
    }

    fun startAnimation() {
        (pendingIcon.background as? Animatable)?.start()
    }

}

package io.horizontalsystems.bankwallet.modules.transactions

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_transaction_progress.view.*


class TransactionProgressView : ConstraintLayout {

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
        inflate(context, R.layout.view_transaction_progress, this)
    }

    fun bind(progress: Double = 0.0, incoming: Boolean) {
        when {
            progress <= 0.33 -> progressBar.setBackgroundResource(R.drawable.animation_pending)
            progress <= 0.66 -> progressBar.setBackgroundResource(if (incoming) R.drawable.animation_receiving_progress_30 else R.drawable.animation_sending_progress_30)
            progress <= 1.0 -> progressBar.setBackgroundResource(if (incoming) R.drawable.animation_receiving_progress_60 else R.drawable.animation_sending_progress_60)
        }
        processText.setText(if (incoming) R.string.Transactions_Receiving else R.string.Transactions_Sending)
        (progressBar.background as? AnimationDrawable)?.start()
    }

}

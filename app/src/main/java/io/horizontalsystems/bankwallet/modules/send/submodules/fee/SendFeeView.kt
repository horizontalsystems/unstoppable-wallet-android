package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import kotlinx.android.synthetic.main.view_fee_priority_input.view.*

class SendFeeView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_fee_priority_input, this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, lifecycleOwner: LifecycleOwner, sendFeeViewModel: SendFeeViewModel, feeIsAdjustable: Boolean) : super(context) {
        val delegate = sendFeeViewModel.delegate

        feeRateSeekbar.visibility = if (feeIsAdjustable) View.VISIBLE else View.GONE

        feeRateSeekbar.bind { progress ->
            delegate.onFeeSliderChange(progress)
        }

        sendFeeViewModel.primaryFee.observe(lifecycleOwner, Observer { txtFeePrimary.text = " $it" })

        sendFeeViewModel.secondaryFee.observe(lifecycleOwner, Observer { txtFeeSecondary.text = it })

        sendFeeViewModel.insufficientFeeBalanceError.observe(lifecycleOwner, Observer { error ->

            if (error != null) {
                feeError.visibility = View.VISIBLE
                feeRateSeekbar.visibility = View.GONE
                feeAmountWrapper.visibility = View.GONE

                val coinCode = error.coin.code
                val tokenProtocol = error.coinProtocol
                val feeCoinTitle = error.feeCoin.title
                val formattedFee = App.numberFormatter.format(error.fee)

                feeError.text = context.getString(R.string.Send_Token_InsufficientFeeAlert, coinCode, tokenProtocol, feeCoinTitle, formattedFee)
            } else {
                feeError.visibility = View.GONE
                feeRateSeekbar.visibility = if (feeIsAdjustable) View.VISIBLE else View.GONE
                feeAmountWrapper.visibility = View.VISIBLE
            }
        })

        delegate.onViewDidLoad()
    }

}

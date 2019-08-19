package io.horizontalsystems.bankwallet.modules.send.sendviews.fee

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
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

        sendFeeViewModel.primaryFeeLiveData.observe(lifecycleOwner, Observer { txtFeePrimary.text = " $it" })

        sendFeeViewModel.secondaryFeeLiveData.observe(lifecycleOwner, Observer { txtFeeSecondary.text = it })


//        viewModel.insufficientFeeBalanceErrorLiveEvent.observe(lifecycleOwner, Observer {feeCoinValue ->
//            feeError.visibility = View.VISIBLE
//            feeRateSeekbar.visibility = View.GONE
//            feeAmountWrapper.visibility = View.GONE
//
//            val coinCode = viewModel.delegate.coinCode
//            val tokenProtocol = viewModel.delegate.tokenProtocol
//            val baseCoinName = viewModel.delegate.baseCoinName
//            val formattedFee = App.numberFormatter.format(feeCoinValue)
//
//            feeError.text = context.getString(R.string.Send_Token_InsufficientFeeAlert, coinCode, tokenProtocol, baseCoinName, formattedFee)
//        })
//
        delegate.onViewDidLoad()
    }

}

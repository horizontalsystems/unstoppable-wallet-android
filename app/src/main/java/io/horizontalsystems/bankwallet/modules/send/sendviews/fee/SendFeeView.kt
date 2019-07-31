package io.horizontalsystems.bankwallet.modules.send.sendviews.fee

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.send.SendViewModel
import kotlinx.android.synthetic.main.view_fee_priority_input.view.*

class SendFeeView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_fee_priority_input, this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private lateinit var viewModel: SendFeeViewModel
    private lateinit var lifecycleOwner: LifecycleOwner

    fun bindInitial(viewModel: SendFeeViewModel, mainViewModel: SendViewModel, lifecycleOwner: LifecycleOwner, feeIsAdjustable: Boolean) {
        this.viewModel = viewModel
        this.lifecycleOwner = lifecycleOwner

        feeRateSeekbar.visibility = if (feeIsAdjustable) View.VISIBLE else View.GONE

        feeRateSeekbar.bind { progress ->
            viewModel.delegate.onFeeSliderChange(progress)
        }

        viewModel.feeIsAdjustableLiveData.observe(lifecycleOwner, Observer{ feeIsAdjustable ->
            feeRateSeekbar.visibility = if (feeIsAdjustable) View.VISIBLE else View.GONE
        })

        viewModel.feePriorityChangeLiveData.observe(lifecycleOwner, Observer { feeRatePriority ->
            mainViewModel.delegate.onFeePriorityChange(feeRatePriority)
        })

        viewModel.primaryFeeLiveData.observe(lifecycleOwner, Observer { txtFeePrimary.text = " $it" })

        viewModel.secondaryFeeLiveData.observe(lifecycleOwner, Observer { txtFeeSecondary.text = it })

        viewModel.insufficientFeeBalanceErrorLiveEvent.observe(lifecycleOwner, Observer {(coinCode, fee) ->
            feeError.visibility = View.VISIBLE
            feeRateSeekbar.visibility = View.GONE
            feeAmountWrapper.visibility = View.GONE
            feeError.text = context.getString(R.string.Send_ERC_Alert, coinCode, fee.stripTrailingZeros().toPlainString())
        })

        viewModel.delegate.onViewDidLoad()
    }

}

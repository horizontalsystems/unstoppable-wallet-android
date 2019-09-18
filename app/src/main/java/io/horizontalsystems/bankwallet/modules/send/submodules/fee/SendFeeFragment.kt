package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.FeeRateInfo
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper
import kotlinx.android.synthetic.main.view_send_fee.*

class SendFeeFragment(private val sendFeeViewModel: SendFeeViewModel,
                      private val feeIsAdjustable: Boolean )
    : Fragment(), FeeRatePrioritySelector.Listener {

    private var delegate: SendFeeModule.IViewDelegate? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.view_send_fee, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        delegate = sendFeeViewModel.delegate

        txSpeedLayout.visibility = if (feeIsAdjustable) View.VISIBLE else View.GONE
        txSpeedLayout.setOnClickListener {
            delegate?.onClickFeeRatePriority()
        }

        sendFeeViewModel.primaryFee.observe(viewLifecycleOwner, Observer { txFeePrimary.text = " $it" })

        sendFeeViewModel.secondaryFee.observe(viewLifecycleOwner, Observer { fiatFee ->
            fiatFee?.let { txFeeSecondary.text = " | $it" }
        })

        sendFeeViewModel.duration.observe(viewLifecycleOwner, Observer { duration ->
            context?.let {
                val txDurationString = DateHelper.getTxDurationString(it, duration)
                txDuration.text = it.getString(R.string.Duration_Within, txDurationString)
            }
        })

        sendFeeViewModel.feePriority.observe(viewLifecycleOwner, Observer { feePriority ->
            context?.let {
                txSpeedMenu.text = TextHelper.getFeeRatePriorityString(it, feePriority)
            }
        })

        sendFeeViewModel.showFeePriorityOptions.observe(viewLifecycleOwner, Observer { feeRates ->
            FeeRatePrioritySelector
                    .newInstance(this, feeRates)
                    .show(this.requireFragmentManager(), "fee_rate_priority_selector")
        })


        sendFeeViewModel.insufficientFeeBalanceError.observe(viewLifecycleOwner, Observer { error ->
            if (error != null) {
                feeError.visibility = View.VISIBLE
                txSpeedLayout.visibility = View.GONE
                feeLayout.visibility = View.GONE

                val coinCode = error.coin.code
                val tokenProtocol = error.coinProtocol
                val feeCoinTitle = error.feeCoin.title
                val formattedFee = App.numberFormatter.format(error.fee)

                feeError.text = context?.getString(R.string.Send_Token_InsufficientFeeAlert, coinCode, tokenProtocol, feeCoinTitle, formattedFee)
            } else {
                feeError.visibility = View.GONE
                txSpeedLayout.visibility = if (feeIsAdjustable) View.VISIBLE else View.GONE
                feeLayout.visibility = View.VISIBLE
            }
        })

        delegate?.onViewDidLoad()
    }

    override fun onSelectFeeRate(feeRate: FeeRateInfo) {
        delegate?.onChangeFeeRate(feeRate)
    }
}

package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.SendSubmoduleFragment
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.ethereumkit.api.models.ApiError
import io.horizontalsystems.seekbar.FeeSeekBar
import kotlinx.android.synthetic.main.view_send_fee.*

class SendFeeFragment(
        private val feeIsAdjustable: Boolean,
        private val coin: Coin,
        private val feeModuleDelegate: SendFeeModule.IFeeModuleDelegate,
        private val sendHandler: SendModule.ISendHandler,
        private val customPriorityUnit: CustomPriorityUnit?)
    : SendSubmoduleFragment() {

    private var presenter: SendFeePresenter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.view_send_fee, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        presenter = ViewModelProvider(this, SendFeeModule.Factory(coin, sendHandler, feeModuleDelegate, customPriorityUnit))
                .get(SendFeePresenter::class.java)
        val presenterView = presenter?.view as SendFeeView

        txError.isVisible = false
        txSpeedLayout.isVisible = feeIsAdjustable
        txSpeedLayout.setOnClickListener {
            presenter?.onClickFeeRatePriority()
        }
        txFeeLoading.isVisible = false
        txFeeLoading.text = getString(R.string.Alert_Loading)

        customFeeSeekBar.setListener(object : FeeSeekBar.Listener {
            override fun onSelect(value: Int) {
                presenter?.onChangeFeeRateValue(value.toLong())
            }
        })

        presenterView.primaryFee.observe(viewLifecycleOwner, Observer { txFeePrimary.text = " $it" })

        presenterView.secondaryFee.observe(viewLifecycleOwner, Observer { fiatFee ->
            fiatFee?.let { txFeeSecondary.text = " | $it" }
        })

        presenterView.duration.observe(viewLifecycleOwner, { duration ->
            duration?.let {
                val txDurationString = DateHelper.getTxDurationString(requireContext(), duration)
                txDuration.text = requireContext().getString(R.string.Duration_Within, txDurationString)
            }
            txDurationLayout.isVisible = duration != null
        })

        presenterView.feePriority.observe(viewLifecycleOwner, Observer { feePriority ->
            context?.let {
                txSpeedMenu.text = TextHelper.getFeeRatePriorityString(it, feePriority)
            }
        })

        presenterView.showFeePriorityOptions.observe(viewLifecycleOwner, Observer { feeRates ->
            val selectorItems = feeRates.map { feeRateViewItem ->
                val feeRateInfo = feeRateViewItem.feeRateInfo
                val selected = feeRateViewItem.selected
                val caption = context?.let { context ->
                    var text = TextHelper.getFeeRatePriorityString(context, feeRateInfo.priority)
                    feeRateInfo.duration?.let {
                        text += " (~${DateHelper.getTxDurationString(context, feeRateInfo.duration)})"
                    }
                    text
                } ?: ""

                SelectorItem(caption, selected)
            }

            SelectorDialog
                    .newInstance(selectorItems, getString(R.string.Send_DialogSpeed)) { position ->
                        presenter?.onChangeFeeRate(feeRates[position].feeRateInfo)
                    }
                    .show(parentFragmentManager, "fee_rate_priority_selector")
        })

        presenterView.showCustomFeePriority.observe(viewLifecycleOwner, Observer { isVisible ->
            customFeeSeekBar.isVisible = isVisible
            txDurationLayout.isVisible = !isVisible
        })

        presenterView.setCustomFeeParams.observe(viewLifecycleOwner, Observer { (value, range, label) ->
            customFeeSeekBar.progress = value
            customFeeSeekBar.min = range.first
            customFeeSeekBar.max = range.last
            label?.let {
                customFeeSeekBar.setBubbleHint(it)
            }
        })

        presenterView.insufficientFeeBalanceError.observe(viewLifecycleOwner, Observer { error ->
            feeError.isVisible = error != null
            feeLayout.isVisible = error == null
            txSpeedLayout.isVisible = error == null && feeIsAdjustable

            if (error != null) {
                val coinCode = error.coin.code
                val tokenProtocol = error.coinProtocol
                val feeCoinTitle = error.feeCoin.title
                val formattedFee = App.numberFormatter.formatCoin(error.fee.value, error.fee.coin.code, 0, 8)

                feeError.text = context?.getString(R.string.Send_Token_InsufficientFeeAlert, coinCode, tokenProtocol,
                                                   feeCoinTitle, formattedFee)
            }
        })

        presenterView.setLoading.observe(viewLifecycleOwner, Observer { loading ->
            setLoading(loading)
        })

        presenterView.setError.observe(viewLifecycleOwner, Observer { error ->
            setError(error)
        })
    }

    override fun init() {
        presenter?.onViewDidLoad()
    }

    private fun setLoading(loading: Boolean) {

        txFeePrimary.isVisible = !loading
        txFeeSecondary.isVisible = !loading
        txFeeLoading.isVisible = loading

        txSpeedMenu.alpha = if (loading) 0.5f else 1f
        txSpeedLayout.isEnabled = (!loading)
    }

    private fun setError(error: Exception) {

        if (error is ApiError)
            txError.text = getString(R.string.Send_Error_WrongParameters)

        txError.isVisible = true
        txFeeTitle.isVisible = false
        txFeeLoading.isVisible = false
        txFeePrimary.isVisible = false
        txFeeSecondary.isVisible = false
    }
}


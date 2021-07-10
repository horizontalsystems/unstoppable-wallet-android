package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.send.SendActivity
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.SendSubmoduleFragment
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.seekbar.FeeSeekBar

class SendFeeFragment(
        private val coin: Coin,
        private val feeModuleDelegate: SendFeeModule.IFeeModuleDelegate,
        private val sendHandler: SendModule.ISendHandler,
        private val customPriorityUnit: CustomPriorityUnit?)
    : SendSubmoduleFragment() {

    private val presenter by activityViewModels<SendFeePresenter> { SendFeeModule.Factory(coin, sendHandler, feeModuleDelegate, customPriorityUnit) }
    private lateinit var txError: TextView
    private lateinit var txFeeLoading: TextView
    private lateinit var customFeeSeekBar: FeeSeekBar
    private lateinit var speedViews: Group
    private lateinit var txFeePrimary: TextView
    private lateinit var txFeeSecondary: TextView
    private lateinit var txSpeedMenu: TextView
    private lateinit var feeError: TextView
    private lateinit var lowFeeWarning: TextView
    private lateinit var txFeeTitle: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.view_send_fee, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        val presenterView = presenter.view as SendFeeView
        txError = view.findViewById(R.id.txError)
        txFeeLoading = view.findViewById(R.id.txFeeLoading)
        customFeeSeekBar = view.findViewById(R.id.customFeeSeekBar)
        speedViews = view.findViewById(R.id.speedViews)
        txFeePrimary = view.findViewById(R.id.txFeePrimary)
        txFeeSecondary = view.findViewById(R.id.txFeeSecondary)
        txSpeedMenu = view.findViewById(R.id.txSpeedMenu)
        feeError = view.findViewById(R.id.feeError)
        lowFeeWarning = view.findViewById(R.id.lowFeeWarning)
        txFeeTitle = view.findViewById(R.id.txFeeTitle)

        txError.isVisible = false

        view.findViewById<View>(R.id.txSpeedMenuClickArea).setOnClickListener {
            presenter.onClickFeeRatePriority()
        }
        view.findViewById<View>(R.id.feeInfoImageClickArea).setOnClickListener {
            (activity as? SendActivity)?.showFeeInfo()
        }
        txFeeLoading.isVisible = false
        txFeeLoading.text = getString(R.string.Alert_Loading)

        customFeeSeekBar.setListener(object : FeeSeekBar.Listener {
            override fun onSelect(value: Int) {
                presenter.onChangeFeeRateValue(value)
            }
        })

        presenterView.showAdjustableFeeMenu.observe(viewLifecycleOwner, Observer { visible ->
            speedViews.isVisible = visible
        })

        presenterView.primaryFee.observe(viewLifecycleOwner, Observer { txFeePrimary.text = " $it" })

        presenterView.secondaryFee.observe(viewLifecycleOwner, Observer { fiatFee ->
            fiatFee?.let { txFeeSecondary.text = " | $it" }
        })

        presenterView.feePriority.observe(viewLifecycleOwner, Observer { feePriority ->
            context?.let {
                txSpeedMenu.text = TextHelper.getFeeRatePriorityString(it, feePriority)
            }
        })

        presenterView.showFeePriorityOptions.observe(viewLifecycleOwner, Observer { feeRates ->
            val selectorItems = feeRates.map { feeRateViewItem ->
                val caption = context?.let { context ->
                    TextHelper.getFeeRatePriorityString(context, feeRateViewItem.feeRatePriority)
                } ?: ""

                SelectorItem(caption, feeRateViewItem.selected)
            }

            SelectorDialog
                    .newInstance(selectorItems, getString(R.string.Send_DialogSpeed)) { position ->
                        presenter.onChangeFeeRate(feeRates[position].feeRatePriority)
                    }
                    .show(parentFragmentManager, "fee_rate_priority_selector")
        })

        presenterView.showCustomFeePriority.observe(viewLifecycleOwner, Observer { isVisible ->
            customFeeSeekBar.isVisible = isVisible
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

        presenterView.showLowFeeWarningLiveData.observe(viewLifecycleOwner, { show ->
            lowFeeWarning.isVisible = show
        })
    }

    override fun init() {
        presenter.onViewDidLoad()
    }

    private fun setLoading(loading: Boolean) {

        txFeePrimary.isVisible = !loading
        txFeeSecondary.isVisible = !loading
        txFeeLoading.isVisible = loading

        txSpeedMenu.alpha = if (loading) 0.5f else 1f
        speedViews.isEnabled = (!loading)
    }

    private fun setError(error: Exception) {
        txError.isVisible = true
        txFeeTitle.isInvisible = true
        txFeeLoading.isVisible = false
        txFeePrimary.isVisible = false
        txFeeSecondary.isVisible = false
    }
}


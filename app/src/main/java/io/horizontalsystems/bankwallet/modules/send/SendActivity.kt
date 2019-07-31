package io.horizontalsystems.bankwallet.modules.send

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.zxing.integration.android.IntentIntegrator
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerModule
import io.horizontalsystems.bankwallet.modules.send.sendviews.address.SendAddressView
import io.horizontalsystems.bankwallet.modules.send.sendviews.address.SendAddressViewModel
import io.horizontalsystems.bankwallet.modules.send.sendviews.amount.SendAmountView
import io.horizontalsystems.bankwallet.modules.send.sendviews.amount.SendAmountViewModel
import io.horizontalsystems.bankwallet.modules.send.sendviews.fee.SendFeeView
import io.horizontalsystems.bankwallet.modules.send.sendviews.fee.SendFeeViewModel
import io.horizontalsystems.bankwallet.modules.send.sendviews.sendbutton.SendButtonView
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import kotlinx.android.synthetic.main.activity_about_settings.shadowlessToolbar
import kotlinx.android.synthetic.main.activity_send.*

class SendActivity : BaseActivity() {

    private var sendAmountViewModel: SendAmountViewModel? = null
    private var sendAddressViewModel: SendAddressViewModel? = null
    private var sendFeeViewModel: SendFeeViewModel? = null
    private lateinit var mainViewModel: SendViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)

        val coinCode: String = intent.getStringExtra(COIN_CODE) ?: ""

        if (coinCode.isEmpty()) {
            finish()
        }

        val iconRes = LayoutHelper.getCoinDrawableResource(coinCode)

        shadowlessToolbar.bind(
                title = getString(R.string.Send_Title, coinCode),
                leftBtnItem = TopMenuItem(iconRes),
                rightBtnItem = TopMenuItem(R.drawable.close) { onBackPressed() }
        )

        mainViewModel = ViewModelProviders.of(this).get(SendViewModel::class.java)
        mainViewModel.init(coinCode)

        observeViewModel()
        addInputItems(coinCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        scanResult?.contents?.let {
            sendAddressViewModel?.delegate?.onAddressScan(it)
        }
    }

    private fun observeViewModel() {
        mainViewModel.availableBalanceRetrievedLiveData.observe(this, Observer { availableBalance ->
            sendAmountViewModel?.delegate?.onAvailableBalanceRetrieved(availableBalance)
        })

        mainViewModel.onAddressParsedLiveData.observe(this, Observer { parsedAddress ->
            sendAddressViewModel?.delegate?.onParsedAddress(parsedAddress)
        })

        mainViewModel.getParamsFromModulesLiveEvent.observe(this, Observer { paramsAction ->
            fetchParamsFromModules(paramsAction)
        })

        mainViewModel.validationErrorLiveEvent.observe(this, Observer { error ->
            sendAmountViewModel?.delegate?.onValidationError(error)
        })

        mainViewModel.insufficientFeeBalanceErrorLiveEvent.observe(this, Observer { (coinCode, fee) ->
            sendFeeViewModel?.delegate?.onInsufficientFeeBalanceError(coinCode, fee)
        })

        mainViewModel.amountValidationLiveEvent.observe(this, Observer {
            sendAmountViewModel?.delegate?.onValidationSuccess()
        })

        mainViewModel.feeUpdatedLiveData.observe(this, Observer { fee ->
            sendFeeViewModel?.delegate?.onFeeUpdated(fee)
        })

        mainViewModel.mainInputTypeUpdatedLiveData.observe(this, Observer { inputType ->
            sendFeeViewModel?.delegate?.onInputTypeUpdated(inputType)
        })

        mainViewModel.showConfirmationLiveEvent.observe(this, Observer {
            ConfirmationFragment.show(this)
        })
    }

    private fun fetchParamsFromModules(paramsAction: SendModule.ParamsAction) {
        val params = mutableMapOf<SendModule.AdapterFields, Any?>()
        params[SendModule.AdapterFields.CoinValue] = sendAmountViewModel?.delegate?.getCoinValue()
        params[SendModule.AdapterFields.CurrencyValue] = sendAmountViewModel?.delegate?.getCurrencyValue()
        params[SendModule.AdapterFields.InputType] = sendAmountViewModel?.delegate?.getInputType()
        params[SendModule.AdapterFields.Address] = sendAddressViewModel?.delegate?.getAddress()
        params[SendModule.AdapterFields.FeeRatePriority] = sendFeeViewModel?.delegate?.getFeePriority()
        params[SendModule.AdapterFields.FeeCoinValue] = sendFeeViewModel?.delegate?.getFeeCoinValue()
        params[SendModule.AdapterFields.FeeCurrencyValue] = sendFeeViewModel?.delegate?.getFeeCurrencyValue()

        mainViewModel.delegate.onParamsFetchedForAction(params, paramsAction)
    }

    private fun addInputItems(coinCode: String) {
        //add amount view
        val sendAmountView = SendAmountView(this)
        sendAmountViewModel = ViewModelProviders.of(this).get(SendAmountViewModel::class.java)
        sendAmountViewModel?.init(coinCode)
        sendAmountViewModel?.let {
            sendAmountView.bindInitial(it, mainViewModel, this, 8)
        }
        sendLinearLayout.addView(sendAmountView)
        sendAmountView.requestFocus()

        //add address view
        val sendAddressView = SendAddressView(this)
        sendAddressViewModel = ViewModelProviders.of(this).get(SendAddressViewModel::class.java)
        sendAddressViewModel?.init()
        sendAddressViewModel?.let {
            sendAddressView.bindAddressInputInitial(it, mainViewModel, this, { QRScannerModule.start(this) })
        }
        sendLinearLayout.addView(sendAddressView)

        //add fee view
        val sendFeeView = SendFeeView(this)
        sendFeeViewModel = ViewModelProviders.of(this).get(SendFeeViewModel::class.java)
        sendFeeViewModel?.init(coinCode)
        sendFeeViewModel?.let {
            sendFeeView.bindInitial(it, mainViewModel, this, true)
        }
        sendLinearLayout.addView(sendFeeView)

        //add send button
        val sendButtonView = SendButtonView(this)
        sendButtonView.bind { mainViewModel.delegate.onSendClicked() }
        sendLinearLayout.addView(sendButtonView)
    }

    companion object{
        const val COIN_CODE = "coin_code_key"
    }

}

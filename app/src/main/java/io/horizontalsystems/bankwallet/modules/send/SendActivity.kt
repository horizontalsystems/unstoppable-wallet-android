package io.horizontalsystems.bankwallet.modules.send

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.zxing.integration.android.IntentIntegrator
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerModule
import io.horizontalsystems.bankwallet.modules.send.sendviews.address.SendAddressViewModel
import io.horizontalsystems.bankwallet.modules.send.sendviews.address.SendInputAddressView
import io.horizontalsystems.bankwallet.modules.send.sendviews.amount.SendAmountView
import io.horizontalsystems.bankwallet.modules.send.sendviews.amount.SendAmountViewModel
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import kotlinx.android.synthetic.main.activity_about_settings.shadowlessToolbar
import kotlinx.android.synthetic.main.activity_send.*

class SendActivity : BaseActivity() {

    private var sendAmountViewModel: SendAmountViewModel? = null
    private var sendAddressViewModel: SendAddressViewModel? = null
    private lateinit var sendMainViewModel: SendViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)

        shadowlessToolbar.bind(
                title = "Send EOS",
                leftBtnItem = TopMenuItem(R.drawable.coin_aura),
                rightBtnItem = TopMenuItem(R.drawable.close) { onBackPressed() }
        )

        sendMainViewModel = ViewModelProviders.of(this).get(SendViewModel::class.java)
        sendMainViewModel.init("BTC")

        sendMainViewModel.availableBalanceRetrievedLiveData.observe(this, Observer { availableBalance ->
            sendAmountViewModel?.delegate?.onAvailableBalanceRetreived(availableBalance)
        })

        sendMainViewModel.onAddressParsedLiveData.observe(this, Observer { parsedAddress ->
            sendAddressViewModel?.delegate?.onParsedAddress(parsedAddress)
        })

        sendMainViewModel.getParamsFromModulesLiveEvent.observe(this, Observer { paramsAction ->
            fetchParamsFromModules(paramsAction)
        })

        addInputItems()
    }

    private fun fetchParamsFromModules(paramsAction: SendModule.ParamsAction) {
        val coinAmount = sendAmountViewModel?.delegate?.getCoinAmount()
        val address = sendAddressViewModel?.delegate?.getAddress()

        val params = mutableMapOf<SendModule.AdapterFields, Any?>()
        params[SendModule.AdapterFields.Amount] = coinAmount
        params[SendModule.AdapterFields.Address] = address

        sendMainViewModel.delegate.onParamsFetchedForAction(params, paramsAction)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        scanResult?.contents?.let {
            sendAddressViewModel?.delegate?.onAddressScan(it)
        }
    }

    private fun addInputItems() {
        //add amount view
        val sendAmountView = SendAmountView(this)
        sendAmountViewModel = ViewModelProviders.of(this).get(SendAmountViewModel::class.java)
        sendAmountViewModel?.init("BTC")
        sendAmountViewModel?.let {
            sendAmountView.bindInitial(it, sendMainViewModel, this, 8)
        }
        sendLinearLayout.addView(sendAmountView)

        //add address view
        val sendAddressView = SendInputAddressView(this)
        sendAddressViewModel = ViewModelProviders.of(this).get(SendAddressViewModel::class.java)
        sendAddressViewModel?.init()
        sendAddressViewModel?.let {
            sendAddressView.bindAddressInputInitial(
                    viewModel = it,
                    mainViewModel = sendMainViewModel,
                    lifecycleOwner = this,
                    onBarcodeClick = { QRScannerModule.start(this) },
                    onAmountChange = { amount -> sendMainViewModel.delegate.onAmountChanged(amount) }
            )
        }

        sendLinearLayout.addView(sendAddressView)
    }


}

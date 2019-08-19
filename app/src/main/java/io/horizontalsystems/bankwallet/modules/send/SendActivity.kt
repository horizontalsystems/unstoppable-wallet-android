package io.horizontalsystems.bankwallet.modules.send

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.zxing.integration.android.IntentIntegrator
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerModule
import io.horizontalsystems.bankwallet.modules.send.sendviews.address.SendAddressView
import io.horizontalsystems.bankwallet.modules.send.sendviews.address.SendAddressViewModel
import io.horizontalsystems.bankwallet.modules.send.sendviews.amount.SendAmountView
import io.horizontalsystems.bankwallet.modules.send.sendviews.amount.SendAmountViewModel
import io.horizontalsystems.bankwallet.modules.send.sendviews.confirmation.ConfirmationFragment
import io.horizontalsystems.bankwallet.modules.send.sendviews.fee.SendFeeView
import io.horizontalsystems.bankwallet.modules.send.sendviews.fee.SendFeeViewModel
import io.horizontalsystems.bankwallet.modules.send.sendviews.sendbutton.SendButtonView
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import kotlinx.android.synthetic.main.activity_send.*

class SendActivity : BaseActivity() {

    private lateinit var mainPresenter: SendModule.IViewDelegate
    private lateinit var mainViewModel: SendViewModel

    private var sendButtonView: SendButtonView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)

        val wallet: Wallet = intent.getParcelableExtra<Wallet>(WALLET) ?: run { finish(); return }

        val iconRes = LayoutHelper.getCoinDrawableResource(wallet.coin.code)

        shadowlessToolbar.bind(
                title = getString(R.string.Send_Title, wallet.coin.title),
                leftBtnItem = TopMenuItem(iconRes),
                rightBtnItem = TopMenuItem(R.drawable.close) { onBackPressed() }
        )

        mainViewModel = ViewModelProviders.of(this).get(SendViewModel::class.java)
        mainPresenter = mainViewModel.init(wallet)

        mainPresenter.onViewDidLoad()

        mainViewModel.inputItemsLiveEvent.observe(this, Observer { inputItems ->
            addInputItems(wallet, inputItems)
        })

        mainViewModel.showSendConfirmationLiveData.observe(this, Observer {
            hideSoftKeyboard()

            val fragmentTransaction = supportFragmentManager
                    .beginTransaction()

            fragmentTransaction
                    .setCustomAnimations(R.anim.slide_in_from_right, R.anim.slide_out_to_right, R.anim.slide_in_from_right, R.anim.slide_out_to_right)
                    .add(R.id.rootView, ConfirmationFragment())
                    .addToBackStack("confirmFragment")
                    .commit()
        })

        mainViewModel.dismissWithSuccessLiveEvent.observe(this, Observer {
            HudHelper.showSuccessMessage(R.string.Send_Success)
            finish()
        })

        mainViewModel.sendButtonEnabledLiveData.observe(this, Observer { enabled ->
            sendButtonView?.updateState(enabled)
        })

        mainViewModel.scanQrCode.observe(this, Observer {
            QRScannerModule.start(this)
        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        scanResult?.contents?.let {
            mainViewModel.delegate.onAddressScan(it)
        }
    }

    private fun addInputItems(wallet: Wallet, inputItems: List<SendModule.Input>) {
        inputItems.forEach { input ->
            when (input) {
                SendModule.Input.Amount -> {
                    //add amount view
                    val amountViewModel = ViewModelProviders.of(this).get(SendAmountViewModel::class.java)
                    val amountPresenter = amountViewModel.init(wallet, mainViewModel.amountModuleDelegate)

                    mainPresenter.amountModule = amountPresenter

                    val amountView = SendAmountView(context = this, lifecycleOwner = this, sendAmountViewModel = amountViewModel)
                    sendLinearLayout.addView(amountView)
                    amountView.requestFocus()

                }
                SendModule.Input.Address -> {
                    //add address view
                    val addressViewModel = ViewModelProviders.of(this).get(SendAddressViewModel::class.java)
                    val addressPresenter = addressViewModel.init(wallet.coin, mainViewModel.addressModuleDelegate)

                    mainPresenter.addressModule = addressPresenter

                    val sendAddressView = SendAddressView(context = this, lifecycleOwner = this, sendAddressViewModel = addressViewModel)
                    sendLinearLayout.addView(sendAddressView)
                }
                is SendModule.Input.Fee -> {
                    //add fee view
                    val feeViewModel = ViewModelProviders.of(this).get(SendFeeViewModel::class.java)
                    val feePresenter = feeViewModel.init(wallet.coin, mainViewModel.feeModuleDelegate)

                    mainPresenter.feeModule = feePresenter

                    val sendFeeView = SendFeeView(context = this, lifecycleOwner = this, sendFeeViewModel = feeViewModel, feeIsAdjustable = input.isAdjustable)
                    sendLinearLayout.addView(sendFeeView)
                }
                SendModule.Input.SendButton -> {
                    //add send button
                    sendButtonView = SendButtonView(this)
                    sendButtonView?.bind { mainPresenter.onSendClicked() }
                    sendLinearLayout.addView(sendButtonView)
                }
            }
        }

        mainPresenter.onModulesDidLoad()
    }

    companion object {
        const val WALLET = "wallet_key"
    }

}

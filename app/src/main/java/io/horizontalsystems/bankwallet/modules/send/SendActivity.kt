package io.horizontalsystems.bankwallet.modules.send

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.zxing.integration.android.IntentIntegrator
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.send.submodules.SendSubmoduleFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.ConfirmationFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.hodler.SendHodlerFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.memo.SendMemoFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.sendbutton.ProceedButtonView
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.activity_send.*
import kotlinx.android.synthetic.main.activity_send.toolbar

class SendActivity : BaseActivity() {

    private lateinit var mainPresenter: SendPresenter

    private var proceedButtonView: ProceedButtonView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)

        val wallet: Wallet = intent.getParcelableExtra(WALLET) ?: run { finish(); return }

        setSupportActionBar(toolbar)

        val coinDrawable = AppLayoutHelper.getCoinDrawable(this, wallet.coin.code, wallet.coin.type)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(coinDrawable)

        title = getString(R.string.Send_Title, wallet.coin.code)

        mainPresenter = ViewModelProvider(this, SendModule.Factory(wallet)).get(SendPresenter::class.java)

        subscribeToViewEvents(mainPresenter.view as SendView, wallet)
        subscribeToRouterEvents(mainPresenter.router as SendRouter)

        mainPresenter.onViewDidLoad()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.send_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuClose -> {
                finish()
                return true
            }
            android.R.id.home -> {
                //don't do anything
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun subscribeToRouterEvents(router: SendRouter) {
        router.closeWithSuccess.observe(this, Observer {
            HudHelper.showSuccessMessage(findViewById(android.R.id.content), R.string.Send_Success, HudHelper.SnackbarDuration.LONG)

            //Delay 1200 millis to properly show message
            Handler().postDelayed({ finish() }, 1200)
        })

        router.scanQrCode.observe(this, Observer {
            QRScannerActivity.start(this)
        })
    }

    private fun subscribeToViewEvents(presenterView: SendView, wallet: Wallet) {
        presenterView.inputItems.observe(this, Observer { inputItems ->
            addInputItems(wallet, inputItems)
        })


        presenterView.showSendConfirmation.observe(this, Observer {
            hideSoftKeyboard()

            supportFragmentManager.commit {
                add(R.id.rootView, ConfirmationFragment(mainPresenter))
                addToBackStack(null)
            }
        })

        presenterView.sendButtonEnabled.observe(this, Observer { enabled ->
            proceedButtonView?.updateState(enabled)
        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IntentIntegrator.REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.getStringExtra(ModuleField.SCAN_ADDRESS)?.let {
                mainPresenter.onAddressScan(it)
            }
        }
    }

    private fun addInputItems(wallet: Wallet, inputItems: List<SendModule.Input>) {
        val fragments = mutableListOf<SendSubmoduleFragment>()

        inputItems.forEach { input ->
            when (input) {
                SendModule.Input.Amount -> {
                    //add amount view
                    mainPresenter.amountModuleDelegate?.let {
                        val sendAmountFragment = SendAmountFragment(wallet, it, mainPresenter.handler)
                        fragments.add(sendAmountFragment)
                        supportFragmentManager.beginTransaction().add(R.id.sendLinearLayout, sendAmountFragment).commitNow()
                    }
                }
                is SendModule.Input.Address -> {
                    //add address view
                    mainPresenter.addressModuleDelegate?.let {
                        val sendAddressFragment = SendAddressFragment(wallet.coin, input.editable, it, mainPresenter.handler)
                        fragments.add(sendAddressFragment)
                        supportFragmentManager.beginTransaction().add(R.id.sendLinearLayout, sendAddressFragment)
                                .commitNow()
                    }
                }
                SendModule.Input.Hodler -> {
                    mainPresenter.hodlerModuleDelegate?.let {
                        val sendAddressFragment = SendHodlerFragment(it, mainPresenter.handler)
                        fragments.add(sendAddressFragment)
                        supportFragmentManager.beginTransaction().add(R.id.sendLinearLayout, sendAddressFragment)
                                .commitNow()
                    }
                }
                is SendModule.Input.Fee -> {
                    //add fee view
                    mainPresenter.feeModuleDelegate?.let {
                        val sendFeeFragment = SendFeeFragment(input.isAdjustable, wallet.coin, it, mainPresenter.handler)
                        fragments.add(sendFeeFragment)
                        supportFragmentManager.beginTransaction().add(R.id.sendLinearLayout, sendFeeFragment)
                                .commitNow()
                    }
                }
                is SendModule.Input.Memo -> {
                    //add memo view
                    val sendMemoFragment = SendMemoFragment(input.maxLength, mainPresenter.handler)
                    fragments.add(sendMemoFragment)
                    supportFragmentManager.beginTransaction().add(R.id.sendLinearLayout, sendMemoFragment).commitNow()
                }
                SendModule.Input.ProceedButton -> {
                    //add send button
                    proceedButtonView = ProceedButtonView(this)
                    proceedButtonView?.bind { mainPresenter.onProceedClicked() }
                    sendLinearLayout.addView(proceedButtonView)
                }
            }
        }

        fragments.forEach { it.init() }

        mainPresenter.onModulesDidLoad()
    }

    companion object {
        const val WALLET = "wallet_key"
    }

}

package io.horizontalsystems.bankwallet.modules.send

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.SendPresenter.ActionState
import io.horizontalsystems.bankwallet.modules.send.submodules.SendSubmoduleFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.ConfirmationFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeInfoFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.hodler.SendHodlerFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.memo.SendMemoFragment
import io.horizontalsystems.bankwallet.modules.send.submodules.sendbutton.ProceedButtonView
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.snackbar.SnackbarDuration
import kotlinx.android.synthetic.main.activity_send.*

class SendActivity : BaseActivity() {

    private lateinit var mainPresenter: SendPresenter

    private var proceedButtonView: ProceedButtonView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // prevent fragment recreations by passing null to onCreate
        super.onCreate(null)
        setContentView(R.layout.activity_send)

       overridePendingTransition(R.anim.slide_from_bottom, R.anim.slide_to_top)

        val wallet: Wallet = intent.getParcelableExtra(WALLET) ?: run { finish(); return }

        toolbar.title = getString(R.string.Send_Title, wallet.coin.code)
        toolbar.navigationIcon = AppLayoutHelper.getCoinDrawable(this, wallet.coin.type)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuClose -> {
                    finish()
                    true
                }
                else -> false
            }
        }

        mainPresenter = ViewModelProvider(this, SendModule.Factory(wallet)).get(SendPresenter::class.java)

        subscribeToViewEvents(mainPresenter.view as SendView, wallet)
        subscribeToRouterEvents(mainPresenter.router as SendRouter)

        mainPresenter.onViewDidLoad()
    }

    private fun subscribeToRouterEvents(router: SendRouter) {
        router.closeWithSuccess.observe(this, Observer {
            HudHelper.showSuccessMessage(findViewById(android.R.id.content), R.string.Send_Success, SnackbarDuration.LONG)

            //Delay 1200 millis to properly show message
            Handler(Looper.getMainLooper()).postDelayed({ finish() }, 1200)
        })
    }

    private fun subscribeToViewEvents(presenterView: SendView, wallet: Wallet) {
        presenterView.inputItems.observe(this, Observer { inputItems ->
            addInputItems(wallet, inputItems)
        })


        presenterView.showSendConfirmation.observe(this, Observer {
            hideSoftKeyboard()

            supportFragmentManager.commit {
                setCustomAnimations(R.anim.slide_from_right, R.anim.slide_to_left, R.anim.slide_from_left, R.anim.slide_to_right)
                add(R.id.rootView, ConfirmationFragment(mainPresenter))
                addToBackStack(null)
            }
        })

        presenterView.sendButtonEnabled.observe(this, Observer { actionState ->
            val defaultTitle = getString(R.string.Send_DialogProceed)

            when (actionState) {
                is ActionState.Enabled -> {
                    proceedButtonView?.updateState(true)
                    proceedButtonView?.setTitle(defaultTitle)
                }
                is ActionState.Disabled -> {
                    proceedButtonView?.updateState(false)
                    proceedButtonView?.setTitle(actionState.title ?: defaultTitle)
                }
            }
        })
    }

    fun showFeeInfo() {
        hideSoftKeyboard()

        supportFragmentManager.commit {
            setCustomAnimations(R.anim.slide_from_right, R.anim.slide_to_left, R.anim.slide_from_left, R.anim.slide_to_right)
            add(R.id.rootView, SendFeeInfoFragment())
            addToBackStack(null)
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
                        val sendAddressFragment = SendAddressFragment(wallet.coin, it, mainPresenter.handler)
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
                        val sendFeeFragment = SendFeeFragment(wallet.coin, it, mainPresenter.handler, mainPresenter.customPriorityUnit)
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

    override fun finish() {
        super.finish()

        overridePendingTransition(0, R.anim.slide_to_bottom)
    }

    companion object {
        const val WALLET = "wallet_key"
    }

}

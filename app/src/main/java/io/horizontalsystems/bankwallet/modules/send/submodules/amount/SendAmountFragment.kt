package io.horizontalsystems.bankwallet.modules.send.submodules.amount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.SendSubmoduleFragment
import kotlinx.android.synthetic.main.view_amount_input.*

class SendAmountFragment(
        private val wallet: Wallet,
        private val amountModuleDelegate: SendAmountModule.IAmountModuleDelegate,
        private val sendHandler: SendModule.ISendHandler)
    : SendSubmoduleFragment() {

    private val presenter by activityViewModels<SendAmountPresenter> { SendAmountModule.Factory(wallet, sendHandler) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.view_amount_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        val presenterView = presenter.view as SendAmountView
        presenter.moduleDelegate = amountModuleDelegate

        amountInput.onTextChangeCallback = { _, new -> presenter.onAmountChange(new ?: "") }
        amountInput.onTapSecondaryCallback = { presenter.onSwitchClick() }
        amountInput.onTapMaxCallback = { presenter.onMaxClick() }
        amountInput.setFocus()

        presenterView.amountInputPrefix.observe(viewLifecycleOwner, Observer { prefix ->
            amountInput.setPrefix(prefix)
        })

        presenterView.amount.observe(viewLifecycleOwner, Observer { amount ->
            amountInput.setAmount(amount)
        })

        presenterView.availableBalance.observe(viewLifecycleOwner, Observer { amount ->
            setAvailableBalance(amount)
        })

        presenterView.hint.observe(viewLifecycleOwner, Observer { hint ->
            amountInput.setSecondaryText(hint)
        })

        presenterView.hintStateEnabled.observe(viewLifecycleOwner, Observer { enabled ->
            amountInput.setSecondaryEnabled(enabled)
        })

        presenterView.maxButtonVisibleValue.observe(viewLifecycleOwner, Observer { visible ->
            amountInput.maxButtonVisible = visible
        })

        presenterView.revertAmount.observe(viewLifecycleOwner, Observer { amount ->
            amountInput.revertAmount(amount)
        })

        presenterView.validationError.observe(viewLifecycleOwner, Observer {
            setValidationError(it)
        })

        presenterView.setLoading.observe(viewLifecycleOwner, Observer { loading ->
            setLoading(loading)
        })
    }

    override fun init() {
        presenter.onViewDidLoad()
    }

    private fun setLoading(loading: Boolean) {
        availableBalanceValue.isInvisible = loading
        processSpinner.isVisible = loading
    }

    private fun setAvailableBalance(availableBalance: String) {
        availableBalanceValue.setText(availableBalance)
    }

    private fun setValidationError(error: SendAmountModule.ValidationError?) {
        processSpinner.isInvisible = true
        txtHintError.isVisible = error != null
        background.hasError = error != null

        txtHintError.text = when (error) {
            is SendAmountModule.ValidationError.InsufficientBalance -> {
                error.availableBalance?.let {
                    context?.getString(R.string.Send_Error_BalanceAmount, it.getFormatted())
                }
            }
            is SendAmountModule.ValidationError.TooFewAmount -> {
                error.minimumAmount?.let {
                    context?.getString(R.string.Send_Error_MinimumAmount, it.getFormatted())
                }
            }
            is SendAmountModule.ValidationError.MaxAmountLimit -> {
                error.maximumAmount?.let {
                    context?.getString(R.string.Send_Error_MaximumAmount, it.getFormatted())
                }
            }
            is SendAmountModule.ValidationError.NotEnoughForMinimumRequiredBalance -> {
                context?.getString(R.string.Send_Error_MinRequiredBalance, App.numberFormatter.formatCoin(error.minimumRequiredBalance.value, error.minimumRequiredBalance.coin.code, 0, 8))
            }
            else -> null
        }
    }

}

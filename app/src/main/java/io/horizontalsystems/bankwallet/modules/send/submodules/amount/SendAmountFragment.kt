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
import io.horizontalsystems.bankwallet.databinding.ViewAmountInputBinding
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.SendSubmoduleFragment

class SendAmountFragment(
    private val wallet: Wallet,
    private val amountModuleDelegate: SendAmountModule.IAmountModuleDelegate,
    private val sendHandler: SendModule.ISendHandler
) : SendSubmoduleFragment() {

    private val presenter by activityViewModels<SendAmountPresenter> {
        SendAmountModule.Factory(wallet, sendHandler)
    }

    private var _binding: ViewAmountInputBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ViewAmountInputBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        val presenterView = presenter.view as SendAmountView
        presenter.moduleDelegate = amountModuleDelegate

        binding.amountInput.onTextChangeCallback = { _, new -> presenter.onAmountChange(new ?: "") }
        binding.amountInput.onTapSecondaryCallback = { presenter.onSwitchClick() }
        binding.amountInput.onTapMaxCallback = { presenter.onMaxClick() }
        binding.amountInput.setFocus()

        presenterView.amount.observe(viewLifecycleOwner, Observer { amount ->
            binding.amountInput.setAmount(amount)
        })

        presenterView.availableBalance.observe(viewLifecycleOwner, Observer { amount ->
            setAvailableBalance(amount)
        })

        presenterView.hint.observe(viewLifecycleOwner, Observer { hint ->
            binding.amountInput.setSecondaryText(hint)
        })

        presenterView.maxButtonVisibleValue.observe(viewLifecycleOwner, Observer { visible ->
            binding.amountInput.maxButtonVisible = visible
        })

        presenterView.revertAmount.observe(viewLifecycleOwner, Observer { amount ->
            binding.amountInput.revertAmount(amount)
        })

        presenterView.validationError.observe(viewLifecycleOwner, Observer {
            setValidationError(it)
        })

        presenterView.setLoading.observe(viewLifecycleOwner, Observer { loading ->
            setLoading(loading)
        })

        presenterView.inputParamsLiveData.observe(viewLifecycleOwner, {
            binding.amountInput.setInputParams(it)
        })
    }

    override fun init() {
        presenter.onViewDidLoad()
    }

    private fun setLoading(loading: Boolean) {
        binding.availableBalanceValue.isInvisible = loading
        binding.processSpinner.isVisible = loading
    }

    private fun setAvailableBalance(availableBalance: String) {
        binding.availableBalanceValue.setText(availableBalance)
    }

    private fun setValidationError(error: SendAmountModule.ValidationError?) {
        binding.processSpinner.isInvisible = true
        binding.txtHintError.isVisible = error != null
        binding.background.hasError = error != null

        binding.txtHintError.text = when (error) {
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
                context?.getString(
                    R.string.Send_Error_MinRequiredBalance,
                    App.numberFormatter.formatCoin(
                        error.minimumRequiredBalance.value,
                        error.minimumRequiredBalance.coin.code,
                        0,
                        8
                    )
                )
            }
            else -> null
        }
    }

}

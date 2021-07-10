package io.horizontalsystems.bankwallet.modules.sendevm

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.sendevm.confirmation.SendEvmConfirmationModule
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.Caution
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.RecipientAddressViewModel
import io.horizontalsystems.bankwallet.ui.extensions.AddressInputView
import io.horizontalsystems.bankwallet.ui.extensions.AmountInputView
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.ViewState

class SendEvmFragment : BaseFragment() {

    private val wallet by lazy { requireArguments().getParcelable<Wallet>(SendEvmModule.walletKey)!! }
    private val vmFactory by lazy { SendEvmModule.Factory(wallet) }
    private val viewModel by navGraphViewModels<SendEvmViewModel>(R.id.sendEvmFragment) { vmFactory }
    private val availableBalanceViewModel by viewModels<SendAvailableBalanceViewModel> { vmFactory }
    private val amountViewModel by viewModels<AmountInputViewModel> { vmFactory }
    private val recipientAddressViewModel by viewModels<RecipientAddressViewModel> { vmFactory }

    private lateinit var txtHintError: TextView
    private lateinit var background: ViewState

    private val qrScannerResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                result.data?.getStringExtra(ModuleField.SCAN_ADDRESS)?.let {
                    recipientAddressViewModel.onFetch(it)
                }
            }
            Activity.RESULT_CANCELED -> {
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_send_evm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        val availableBalanceSpinner = view.findViewById<ProgressBar>(R.id.availableBalanceSpinner)
        val availableBalanceValue = view.findViewById<TextView>(R.id.availableBalanceValue)
        val amountInput = view.findViewById<AmountInputView>(R.id.amountInput)
        val btnProceed = view.findViewById<Button>(R.id.btnProceed)
        txtHintError = view.findViewById(R.id.txtHintError)
        background = view.findViewById(R.id.background)

        toolbar.title = getString(R.string.Send_Title, wallet.coin.code)
        toolbar.navigationIcon = AppLayoutHelper.getCoinDrawable(requireContext(), wallet.coin.type)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuClose -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        availableBalanceViewModel.viewStateLiveData.observe(viewLifecycleOwner, { state ->
            availableBalanceSpinner.isVisible = state is SendAvailableBalanceViewModel.ViewState.Loading
            availableBalanceValue.text = (state as? SendAvailableBalanceViewModel.ViewState.Loaded)?.value
        })

        amountInput.onTextChangeCallback = { _, new -> amountViewModel.onChangeAmount(new ?: "") }
        amountInput.onTapSecondaryCallback = { amountViewModel.onSwitch() }
        amountInput.onTapMaxCallback = { amountViewModel.onClickMax() }
        amountInput.postDelayed({ amountInput.setFocus()}, 200)


        amountViewModel.amountLiveData.observe(viewLifecycleOwner, { amount ->
            if (amountInput.getAmount() != amount && !amountViewModel.areAmountsEqual(amountInput.getAmount(), amount))
                amountInput.setAmount(amount)
        })

        amountViewModel.revertAmountLiveData.observe(viewLifecycleOwner, { revertAmount ->
            amountInput.revertAmount(revertAmount)
        })

        amountViewModel.maxEnabledLiveData.observe(viewLifecycleOwner, { maxEnabled ->
            amountInput.maxButtonVisible = maxEnabled
        })

        amountViewModel.secondaryTextLiveData.observe(viewLifecycleOwner, { secondaryText ->
            amountInput.setSecondaryText(secondaryText)
        })

        amountViewModel.inputParamsLiveData.observe(viewLifecycleOwner, {
            amountInput.setInputParams(it)
        })

        viewModel.amountCautionLiveData.observe(viewLifecycleOwner, { caution ->
            setAmountInputCaution(caution)
        })

        view.findViewById<AddressInputView>(R.id.recipientAddressInputView).setViewModel(recipientAddressViewModel, viewLifecycleOwner, {
            val intent = QRScannerActivity.getIntentForFragment(this)
            qrScannerResultLauncher.launch(intent)
        })

        btnProceed.setOnSingleClickListener {
            viewModel.onClickProceed()
        }

        viewModel.proceedEnabledLiveData.observe(viewLifecycleOwner, { enabled ->
            btnProceed.isEnabled = enabled
        })

        viewModel.proceedLiveEvent.observe(viewLifecycleOwner, { sendData ->
            SendEvmConfirmationModule.start(this, R.id.sendEvmFragment_to_sendEvmConfirmationFragment, navOptions(), sendData)
        })
    }

    private fun setAmountInputCaution(caution: Caution?) {
        txtHintError.isVisible = caution != null
        txtHintError.text = caution?.text
        background.hasError = caution != null

        when (caution?.type) {
            Caution.Type.Error -> {
                background.hasError = true
                txtHintError.setTextColor(requireContext().getColor(R.color.red_d))
            }
            Caution.Type.Warning -> {
                background.hasWarning = true
                txtHintError.setTextColor(requireContext().getColor(R.color.yellow_d))
            }
            else -> {
                background.clearStates()
            }
        }
    }

}

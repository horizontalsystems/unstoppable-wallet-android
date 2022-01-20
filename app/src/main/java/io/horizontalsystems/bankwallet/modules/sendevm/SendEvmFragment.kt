package io.horizontalsystems.bankwallet.modules.sendevm

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.databinding.FragmentSendEvmBinding
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.sendevm.confirmation.SendEvmConfirmationModule
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.bankwallet.modules.swap.settings.RecipientAddressViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.FullCoin

class SendEvmFragment : BaseFragment() {

    private val wallet by lazy { requireArguments().getParcelable<Wallet>(SendEvmModule.walletKey)!! }
    private val vmFactory by lazy { SendEvmModule.Factory(wallet) }
    private val viewModel by navGraphViewModels<SendEvmViewModel>(R.id.sendEvmFragment) { vmFactory }
    private val availableBalanceViewModel by viewModels<SendAvailableBalanceViewModel> { vmFactory }
    private val amountViewModel by viewModels<AmountInputViewModel> { vmFactory }
    private val recipientAddressViewModel by viewModels<RecipientAddressViewModel> { vmFactory }

    private val qrScannerResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    result.data?.getStringExtra(ModuleField.SCAN_ADDRESS)?.let {
                        binding.recipientAddressInputView.setText(it)
                    }
                }
                Activity.RESULT_CANCELED -> {
                }
            }
        }

    private var _binding: FragmentSendEvmBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSendEvmBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setToolbar(wallet.platformCoin.fullCoin)

        availableBalanceViewModel.viewStateLiveData.observe(viewLifecycleOwner, { state ->
            binding.availableBalanceSpinner.isVisible =
                state is SendAvailableBalanceViewModel.ViewState.Loading
            binding.availableBalanceValue.text =
                (state as? SendAvailableBalanceViewModel.ViewState.Loaded)?.value
        })

        binding.amountInput.onTextChangeCallback =
            { _, new -> amountViewModel.onChangeAmount(new ?: "") }
        binding.amountInput.onTapSecondaryCallback = { amountViewModel.onSwitch() }
        binding.amountInput.onTapMaxCallback = { amountViewModel.onClickMax() }
        binding.amountInput.postDelayed({ binding.amountInput.setFocus() }, 200)


        amountViewModel.amountLiveData.observe(viewLifecycleOwner, { amount ->
            if (binding.amountInput.getAmount() != amount && !amountViewModel.areAmountsEqual(
                    binding.amountInput.getAmount(),
                    amount
                )
            )
                binding.amountInput.setAmount(amount)
        })

        amountViewModel.revertAmountLiveData.observe(viewLifecycleOwner, { revertAmount ->
            binding.amountInput.revertAmount(revertAmount)
        })

        amountViewModel.maxEnabledLiveData.observe(viewLifecycleOwner, { maxEnabled ->
            binding.amountInput.maxButtonVisible = maxEnabled
        })

        amountViewModel.secondaryTextLiveData.observe(viewLifecycleOwner, { secondaryText ->
            binding.amountInput.setSecondaryText(secondaryText)
        })

        amountViewModel.inputParamsLiveData.observe(viewLifecycleOwner, {
            binding.amountInput.setInputParams(it)
        })

        viewModel.amountCautionLiveData.observe(viewLifecycleOwner, { caution ->
            setAmountInputCaution(caution)
        })

        binding.recipientAddressInputView.setViewModel(
            recipientAddressViewModel,
            viewLifecycleOwner,
            {
                val intent = QRScannerActivity.getIntentForFragment(this)
                qrScannerResultLauncher.launch(intent)
            })

        viewModel.proceedEnabledLiveData.observe(viewLifecycleOwner, { enabled ->
            setProceedButton(enabled)
        })

        viewModel.proceedLiveEvent.observe(viewLifecycleOwner, { sendData ->
            findNavController().slideFromRight(
                R.id.sendEvmFragment_to_sendEvmConfirmationFragment,
                SendEvmConfirmationModule.prepareParams(sendData)
            )
        })

        binding.buttonProceedCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        setProceedButton()
    }

    private fun setToolbar(fullCoin: FullCoin) {
        binding.toolbarCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this)
        )
        binding.toolbarCompose.setContent {
            ComposeAppTheme {
                AppBar(
                    title = TranslatableString.ResString(R.string.Send_Title, fullCoin.coin.code),
                    navigationIcon = {
                        CoinImage(
                            iconUrl = fullCoin.coin.iconUrl,
                            placeholder = fullCoin.iconPlaceholder,
                            modifier = Modifier.padding(horizontal = 16.dp).size(24.dp)
                        )
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = {
                                findNavController().popBackStack()
                            }
                        )
                    )
                )
            }
        }
    }

    private fun setAmountInputCaution(caution: Caution?) {
        binding.txtHintError.isVisible = caution != null
        binding.txtHintError.text = caution?.text
        binding.background.hasError = caution != null

        when (caution?.type) {
            Caution.Type.Error -> {
                binding.background.hasError = true
                binding.txtHintError.setTextColor(requireContext().getColor(R.color.red_d))
            }
            Caution.Type.Warning -> {
                binding.background.hasWarning = true
                binding.txtHintError.setTextColor(requireContext().getColor(R.color.yellow_d))
            }
            else -> {
                binding.background.clearStates()
            }
        }
    }

    private fun setProceedButton(enabled: Boolean = false) {
        binding.buttonProceedCompose.setContent {
            ComposeAppTheme {
                ButtonPrimaryYellow(
                    modifier = Modifier.padding(
                        top = 24.dp,
                        bottom = 24.dp
                    ),
                    title = getString(R.string.Send_DialogProceed),
                    onClick = {
                        viewModel.onClickProceed()
                    },
                    enabled = enabled
                )
            }
        }
    }

}

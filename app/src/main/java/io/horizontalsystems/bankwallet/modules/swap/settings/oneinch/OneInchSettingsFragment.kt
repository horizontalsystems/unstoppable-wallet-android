package io.horizontalsystems.bankwallet.modules.swap.settings.oneinch

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.databinding.FragmentSwapSettings1inchBinding
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.swap.oneinch.OneInchModule
import io.horizontalsystems.bankwallet.modules.swap.oneinch.OneInchSwapViewModel
import io.horizontalsystems.bankwallet.modules.swap.settings.RecipientAddressViewModel
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsBaseFragment
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSlippageViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class OneInchSettingsFragment : SwapSettingsBaseFragment() {

    private val oneInchViewModel by navGraphViewModels<OneInchSwapViewModel>(R.id.swapFragment) {
        OneInchModule.Factory(
            dex
        )
    }

    private val vmFactory by lazy {
        OneInchSwapSettingsModule.Factory(
            oneInchViewModel.tradeService,
            dex.blockchain
        )
    }
    private val oneInchSettingsViewModel by viewModels<OneInchSettingsViewModel> { vmFactory }
    private val recipientAddressViewModel by viewModels<RecipientAddressViewModel> { vmFactory }
    private val slippageViewModel by viewModels<SwapSlippageViewModel> { vmFactory }

    private val qrScannerResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    result.data?.getStringExtra(ModuleField.SCAN_ADDRESS)?.let {
                        binding.recipientAddressInputView.setText(it)
                    }
                }
                Activity.RESULT_CANCELED -> {
                    findNavController().popBackStack()
                }
            }
        }

    private var _binding: FragmentSwapSettings1inchBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSwapSettings1inchBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        oneInchSettingsViewModel.actionStateLiveData.observe(viewLifecycleOwner) { actionState ->
            when (actionState) {
                is OneInchSettingsViewModel.ActionState.Enabled -> {
                    setButton(getString(R.string.SwapSettings_Apply), true)
                }
                is OneInchSettingsViewModel.ActionState.Disabled -> {
                    setButton(actionState.title, false)
                }
            }
        }

        binding.recipientAddressInputView.setViewModel(
            recipientAddressViewModel,
            viewLifecycleOwner,
            {
                val intent = QRScannerActivity.getIntentForFragment(this)
                qrScannerResultLauncher.launch(intent)
            })

        binding.slippageInputView.setViewModel(slippageViewModel, viewLifecycleOwner)

        binding.buttonApplyCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )
    }

    private fun setButton(title: String, enabled: Boolean = false) {
        binding.buttonApplyCompose.setContent {
            ComposeAppTheme {
                ButtonPrimaryYellow(
                    modifier = Modifier.padding(
                        top = 28.dp,
                        bottom = 24.dp
                    ),
                    title = title,
                    onClick = {
                        if (oneInchSettingsViewModel.onDoneClick()) {
                            findNavController().popBackStack()
                        } else {
                            HudHelper.showErrorMessage(
                                this.requireView(),
                                getString(R.string.default_error_msg)
                            )
                        }
                    },
                    enabled = enabled
                )
            }
        }
    }

}

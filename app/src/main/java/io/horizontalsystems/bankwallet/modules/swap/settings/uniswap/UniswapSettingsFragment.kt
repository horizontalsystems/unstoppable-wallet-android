package io.horizontalsystems.bankwallet.modules.swap.settings.uniswap

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.swap.settings.RecipientAddressViewModel
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapDeadlineViewModel
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsBaseFragment
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSlippageViewModel
import io.horizontalsystems.bankwallet.modules.swap.settings.uniswap.UniswapSettingsViewModel.ActionState
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapModule
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapViewModel
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_swap_settings_uniswap.*

class UniswapSettingsFragment : SwapSettingsBaseFragment() {
    private val uniswapViewModel by navGraphViewModels<UniswapViewModel>(R.id.swapFragment) { UniswapModule.Factory(dex) }

    private val vmFactory by lazy { UniswapSettingsModule.Factory(uniswapViewModel.tradeService, dex) }
    private val uniswapSettingsViewModel by viewModels<UniswapSettingsViewModel> { vmFactory }
    private val deadlineViewModel by viewModels<SwapDeadlineViewModel> { vmFactory }
    private val recipientAddressViewModel by viewModels<RecipientAddressViewModel> { vmFactory }
    private val slippageViewModel by viewModels<SwapSlippageViewModel> { vmFactory }

    private val qrScannerResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                result.data?.getStringExtra(ModuleField.SCAN_ADDRESS)?.let {
                    recipientAddressViewModel.onFetch(it)
                }
            }
            Activity.RESULT_CANCELED -> {
                findNavController().popBackStack()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_swap_settings_uniswap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uniswapSettingsViewModel.actionStateLiveData.observe(viewLifecycleOwner) { actionState ->
            when (actionState) {
                is ActionState.Enabled -> {
                    applyButton.isEnabled = true
                    applyButton.text = getString(R.string.SwapSettings_Apply)
                }
                is ActionState.Disabled -> {
                    applyButton.isEnabled = false
                    applyButton.text = actionState.title
                }
            }
        }

        applyButton.setOnSingleClickListener {
            if (uniswapSettingsViewModel.onDoneClick()) {
                findNavController().popBackStack()
            } else {
                HudHelper.showErrorMessage(this.requireView(), getString(R.string.default_error_msg))
            }
        }

        recipientAddressInputView.setViewModel(recipientAddressViewModel, viewLifecycleOwner, {
            val intent = QRScannerActivity.getIntentForFragment(this)
            qrScannerResultLauncher.launch(intent)
        })

        slippageInputView.setViewModel(slippageViewModel, viewLifecycleOwner)
        deadlineInputView.setViewModel(deadlineViewModel, viewLifecycleOwner)
    }

}

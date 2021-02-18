package io.horizontalsystems.bankwallet.modules.swap.tradeoptions

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.swap.SwapViewModel
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.SwapTradeOptionsViewModel.ActionState
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_swap_settings.*

class SwapTradeOptionsFragment : BaseFragment() {
    private val swapViewModel by navGraphViewModels<SwapViewModel>(R.id.swapFragment)

    private val vmFactory by lazy { SwapTradeOptionsModule.Factory(swapViewModel.tradeService) }
    private val viewModel by viewModels<SwapTradeOptionsViewModel> { vmFactory }
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
        return inflater.inflate(R.layout.fragment_swap_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuCancel -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        viewModel.actionStateLiveData.observe(viewLifecycleOwner) { actionState ->
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
            if (viewModel.onDoneClick()) {
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

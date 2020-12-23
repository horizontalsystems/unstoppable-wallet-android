package io.horizontalsystems.bankwallet.modules.swap.tradeoptions

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import com.google.zxing.integration.android.IntentIntegrator
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.swap.SwapViewModel
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

        viewModel.buttonEnableStateLiveData.observe(viewLifecycleOwner, {
            applyButton.isEnabled = it
        })

        viewModel.applyStateLiveData.observe(viewLifecycleOwner, { isProcessed ->
            if (isProcessed) {
                findNavController().popBackStack()
            } else {
                HudHelper.showErrorMessage(this.requireView(), getString(R.string.default_error_msg))
            }
        })

        applyButton.setOnSingleClickListener {
            viewModel.onClickApply()
        }

        recipientAddressInputView.setViewModel(recipientAddressViewModel, viewLifecycleOwner, {
            QRScannerActivity.start(this)
        })

        slippageInputView.setViewModel(slippageViewModel, viewLifecycleOwner)
        deadlineInputView.setViewModel(deadlineViewModel, viewLifecycleOwner)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    data?.getStringExtra(ModuleField.SCAN_ADDRESS)?.let {
                        recipientAddressInputView.setText(it)
                    }
                }
                Activity.RESULT_CANCELED -> {
                    findNavController().popBackStack()
                }
            }
        }
    }

}

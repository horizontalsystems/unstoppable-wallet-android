package io.horizontalsystems.bankwallet.modules.swap.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import com.google.zxing.integration.android.IntentIntegrator
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsModule.SwapSettings
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.setNavigationResult
import kotlinx.android.synthetic.main.fragment_swap_settings.*

class SwapSettingsFragment : BaseFragment() {

    val viewModel by viewModels<SwapSettingsViewModel> {
        val currentSwapSettings = arguments?.getParcelable<SwapSettings>(swapSettingsKey)!!
        val defaultSwapSettings = arguments?.getParcelable<SwapSettings>(defaultSwapSettingsKey)!!
        SwapSettingsModule.Factory(currentSwapSettings, defaultSwapSettings)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_swap_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        slippageInputView.apply {
            onTextChange { viewModel.setSlippage(it) }
            onLeftButtonClick { viewModel.onSlippageLeftButtonClick() }
            onRightButtonClick { viewModel.onSlippageRightButtonClick() }
        }

        deadlineInputView.apply {
            onTextChange { viewModel.setDeadline(it) }
            onLeftButtonClick { viewModel.onDeadlineLeftButtonClick() }
            onRightButtonClick { viewModel.onDeadlineRightButtonClick() }
        }

        recipientAddressInputView.apply {
            onTextChange { viewModel.setRecipientAddress(it) }
            onButtonQrScanClick { QRScannerActivity.start(this@SwapSettingsFragment) }
        }

        observeViewModel()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.swap_settings_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuCancel -> {
                findNavController().popBackStack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun observeViewModel() {
        viewModel.slippageButtonTitles.observe(viewLifecycleOwner, { (leftButtonTitle, rightButtonTitle) ->
            slippageInputView.setLeftButtonTitle(leftButtonTitle)
            slippageInputView.setRightButtonTitle(rightButtonTitle)
        })

        viewModel.slippage.observe(viewLifecycleOwner, { value ->
            slippageInputView.setText(value)
        })

        viewModel.slippageHint.observe(viewLifecycleOwner, { hint ->
            slippageInputView.setHint(hint)
        })

        viewModel.slippageError.observe(viewLifecycleOwner, { error ->
            slippageInputView.setError(error)
        })

        viewModel.deadlineButtonTitles.observe(viewLifecycleOwner, { (leftButtonTitle, rightButtonTitle) ->
            deadlineInputView.setLeftButtonTitle(leftButtonTitle)
            deadlineInputView.setRightButtonTitle(rightButtonTitle)
        })

        viewModel.deadline.observe(viewLifecycleOwner, { value ->
            deadlineInputView.setText(value)
        })

        viewModel.deadlineHint.observe(viewLifecycleOwner, { hint ->
            deadlineInputView.setHint(hint)
        })

        viewModel.deadlineError.observe(viewLifecycleOwner, { error ->
            deadlineInputView.setError(error)
        })

        viewModel.recipientAddress.observe(viewLifecycleOwner, { value ->
            recipientAddressInputView.setText(value)
        })

        viewModel.recipientAddressHint.observe(viewLifecycleOwner, { hint ->
            recipientAddressInputView.setHint(hint)
        })

        viewModel.recipientAddressError.observe(viewLifecycleOwner, { error ->
            recipientAddressInputView.setError(error)
        })

        viewModel.enableApply.observe(viewLifecycleOwner, { swapSettings ->
            applyButton.apply {
                isEnabled = swapSettings != null
                setOnClickListener {
                    val bundle = bundleOf(resultKey to true, swapSettingsKey to swapSettings)
                    setNavigationResult(requestKey, bundle)
                    findNavController().popBackStack()
                }
            }
        })
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
            }
        }
    }

    companion object {
        const val requestKey = "swapSettingsRequestKey"
        const val resultKey = "swapSettingsResultKey"
        const val swapSettingsKey = "swapSettingsKey"
        const val defaultSwapSettingsKey = "defaultSwapSettingsKey"

        fun params(currentSettings: SwapSettings, defaultSettings: SwapSettings): Bundle {
            return Bundle(1).apply {
                putParcelable(swapSettingsKey, currentSettings)
                putParcelable(defaultSwapSettingsKey, defaultSettings)
            }
        }
    }

}

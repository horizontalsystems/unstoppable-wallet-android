package io.horizontalsystems.bankwallet.modules.createaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.ui.selector.SelectorOptionTextViewHolderFactory
import io.horizontalsystems.bankwallet.ui.selector.SelectorPopupDialog
import io.horizontalsystems.bankwallet.ui.selector.ViewItemWrapper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_create_account.*

class CreateAccountFragment : BaseFragment() {
    private val viewModel by viewModels<CreateAccountViewModel> { CreateAccountModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_create_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.create -> {
                    viewModel.onClickCreate()
                    true
                }
                else -> false
            }
        }

        viewModel.kindLiveData.observe(viewLifecycleOwner) {
            kind.showValue(it)
        }

        viewModel.inputsVisibleLiveData.observe(viewLifecycleOwner) {
            passphrase.isVisible = it
            passphraseConfirm.isVisible = it
            passphraseDescription.isVisible = it
        }

        viewModel.finishLiveEvent.observe(viewLifecycleOwner) {
            findNavController().popBackStack()
            HudHelper.showSuccessMessage(requireView(), getString(R.string.Hud_Text_Created))
        }

        viewModel.showErrorLiveEvent.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireView(), it)
        }

        viewModel.passphraseCautionLiveData.observe(viewLifecycleOwner) {
            passphrase.setError(it)
        }

        viewModel.passphraseConfirmationCautionLiveData.observe(viewLifecycleOwner) {
            passphraseConfirm.setError(it)
        }

        viewModel.clearInputsLiveEvent.observe(viewLifecycleOwner) {
            passphrase.setText(null)
            passphraseConfirm.setText(null)
        }

        kind.setOnSingleClickListener {
            val dialog = SelectorPopupDialog<ViewItemWrapper<CreateAccountModule.Kind>>()

            dialog.titleText = getString(R.string.CreateWallet_Mnemonic)
            dialog.items = viewModel.kindViewItems
            dialog.selectedItem = viewModel.selectedKindViewItem
            dialog.onSelectListener = {
                viewModel.selectedKindViewItem = it
            }
            dialog.itemViewHolderFactory = SelectorOptionTextViewHolderFactory()

            dialog.show(childFragmentManager, "selector_dialog")
        }

        passphraseToggle.setOnCheckedChangeListenerSingle {
            viewModel.onTogglePassphrase(it)
        }

        passphrase.onTextChange { old, new ->
            if (viewModel.validatePassphrase(new)) {
                viewModel.onChangePassphrase(new ?: "")
            } else {
                passphrase.revertText(old)
            }
        }

        passphraseConfirm.onTextChange { old, new ->
            if (viewModel.validatePassphraseConfirmation(new)) {
                viewModel.onChangePassphraseConfirmation(new ?: "")
            } else {
                passphraseConfirm.revertText(old)
            }
        }
    }
}

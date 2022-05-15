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
import io.horizontalsystems.bankwallet.databinding.FragmentCreateAccountBinding
import io.horizontalsystems.bankwallet.ui.selector.SelectorOptionTextViewHolderFactory
import io.horizontalsystems.bankwallet.ui.selector.SelectorPopupDialog
import io.horizontalsystems.bankwallet.ui.selector.ViewItemWrapper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class CreateAccountFragment : BaseFragment() {
    private val viewModel by viewModels<CreateAccountViewModel> { CreateAccountModule.Factory() }

    private var _binding: FragmentCreateAccountBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateAccountBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.create -> {
                    viewModel.onClickCreate()
                    true
                }
                else -> false
            }
        }

        viewModel.kindLiveData.observe(viewLifecycleOwner) {
            binding.kind.showValue(it)
        }

        viewModel.inputsVisibleLiveData.observe(viewLifecycleOwner) {
            binding.passphrase.isVisible = it
            binding.passphraseConfirm.isVisible = it
            binding.passphraseDescription.isVisible = it
        }

        viewModel.finishLiveEvent.observe(viewLifecycleOwner) {
            findNavController().popBackStack()
            HudHelper.showSuccessMessage(requireView(), getString(R.string.Hud_Text_Created))
        }

        viewModel.showErrorLiveEvent.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireView(), it)
        }

        viewModel.passphraseCautionLiveData.observe(viewLifecycleOwner) {
            binding.passphrase.setError(it)
        }

        viewModel.passphraseConfirmationCautionLiveData.observe(viewLifecycleOwner) {
            binding.passphraseConfirm.setError(it)
        }

        viewModel.clearInputsLiveEvent.observe(viewLifecycleOwner) {
            binding.passphrase.setText(null)
            binding.passphraseConfirm.setText(null)
        }

        binding.kind.setOnSingleClickListener {
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

        binding.passphraseToggle.setOnCheckedChangeListenerSingle {
            viewModel.onTogglePassphrase(it)
        }

        binding.passphrase.onTextChange { old, new ->
            if (viewModel.validatePassphrase(new)) {
                viewModel.onChangePassphrase(new ?: "")
            } else {
                binding.passphrase.revertText(old)
            }
        }

        binding.passphraseConfirm.onTextChange { old, new ->
            if (viewModel.validatePassphraseConfirmation(new)) {
                viewModel.onChangePassphraseConfirmation(new ?: "")
            } else {
                binding.passphraseConfirm.revertText(old)
            }
        }
    }
}

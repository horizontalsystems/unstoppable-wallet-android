package io.horizontalsystems.bankwallet.modules.backupconfirmkey

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.databinding.FragmentBackupWordsConfirmBinding
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.helpers.KeyboardHelper

class BackupConfirmKeyFragment : BaseFragment() {
    private val viewModel by viewModels<BackupConfirmKeyViewModel> {
        BackupConfirmKeyModule.Factory(
            arguments?.getParcelable(BackupConfirmKeyModule.ACCOUNT)!!
        )
    }

    private var _binding: FragmentBackupWordsConfirmBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupWordsConfirmBinding.inflate(inflater, container, false)
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
                R.id.itemDone -> {
                    onClickDone()
                    true
                }
                else -> false
            }
        }

        binding.passphrase.isVisible = viewModel.passpraseVisible
        binding.passphraseDescription.isVisible = viewModel.passpraseVisible

        viewModel.indexViewItemLiveData.observe(viewLifecycleOwner, { indexViewItem ->
            binding.wordOne.bindPrefix(indexViewItem.first)
            binding.wordTwo.bindPrefix(indexViewItem.second)
        })

        viewModel.successLiveEvent.observe(viewLifecycleOwner, {
            HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Done)
            Handler(Looper.getMainLooper()).postDelayed({
                findNavController().popBackStack(R.id.backupKeyFragment, true)
            }, 1200)
        })

        viewModel.firstWordCautionLiveData.observe(viewLifecycleOwner) {
            binding.wordOne.setError(it)
        }

        viewModel.secondWordCautionLiveData.observe(viewLifecycleOwner) {
            binding.wordTwo.setError(it)
        }

        viewModel.passphraseCautionLiveData.observe(viewLifecycleOwner) {
            binding.passphrase.setError(it)
        }

        viewModel.clearInputsLiveEvent.observe(viewLifecycleOwner) {
            binding.wordOne.setText(null)
            binding.wordTwo.setText(null)
            binding.passphrase.setText(null)
        }

        binding.wordOne.onTextChange { _, text ->
            viewModel.onChangeFirstWord(text ?: "")
        }

        binding.wordTwo.onTextChange { _, text ->
            viewModel.onChangeSecondWord(text ?: "")
        }

        binding.passphrase.onTextChange { _, text ->
            viewModel.onChangePassphrase(text ?: "")
        }

        KeyboardHelper.showKeyboardDelayed(requireActivity(), binding.wordOne, 200)

        viewModel.onViewCreated()
    }

    private fun onClickDone() {
        viewModel.onClickDone()
    }

}

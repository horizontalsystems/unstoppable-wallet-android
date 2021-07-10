package io.horizontalsystems.bankwallet.modules.backupconfirmkey

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.ui.extensions.InputView
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.helpers.KeyboardHelper

class BackupConfirmKeyFragment : BaseFragment() {
    private val viewModel by viewModels<BackupConfirmKeyViewModel> { BackupConfirmKeyModule.Factory(arguments?.getParcelable(BackupConfirmKeyModule.ACCOUNT)!!) }

    private lateinit var toolbar: Toolbar
    private lateinit var passphrase: InputView
    private lateinit var passphraseDescription: TextView
    private lateinit var wordOne: InputView
    private lateinit var wordTwo: InputView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_words_confirm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.toolbar)
        passphrase = view.findViewById(R.id.passphrase)
        passphraseDescription = view.findViewById(R.id.passphraseDescription)
        wordOne = view.findViewById(R.id.wordOne)
        wordTwo = view.findViewById(R.id.wordTwo)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.itemDone -> {
                    onClickDone()
                    true
                }
                else -> false
            }
        }

        passphrase.isVisible = viewModel.passpraseVisible
        passphraseDescription.isVisible = viewModel.passpraseVisible

        viewModel.indexViewItemLiveData.observe(viewLifecycleOwner, { indexViewItem ->
            wordOne.bindPrefix(indexViewItem.first)
            wordTwo.bindPrefix(indexViewItem.second)
        })

        viewModel.successLiveEvent.observe(viewLifecycleOwner, {
            HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Success)
            Handler(Looper.getMainLooper()).postDelayed({
                findNavController().popBackStack(R.id.backupKeyFragment, true)
            }, 1200)
        })

        viewModel.firstWordCautionLiveData.observe(viewLifecycleOwner) {
            wordOne.setError(it)
        }

        viewModel.secondWordCautionLiveData.observe(viewLifecycleOwner) {
            wordTwo.setError(it)
        }

        viewModel.passphraseCautionLiveData.observe(viewLifecycleOwner) {
            passphrase.setError(it)
        }

        viewModel.clearInputsLiveEvent.observe(viewLifecycleOwner) {
            wordOne.setText(null)
            wordTwo.setText(null)
            passphrase.setText(null)
        }

        wordOne.onTextChange { _, text ->
            viewModel.onChangeFirstWord(text ?: "")
        }

        wordTwo.onTextChange { _, text ->
            viewModel.onChangeSecondWord(text ?: "")
        }

        passphrase.onTextChange { _, text ->
            viewModel.onChangePassphrase(text ?: "")
        }

        KeyboardHelper.showKeyboardDelayed(requireActivity(), wordOne, 200)

        viewModel.onViewCreated()
    }

    private fun onClickDone() {
        viewModel.onClickDone()
    }

}

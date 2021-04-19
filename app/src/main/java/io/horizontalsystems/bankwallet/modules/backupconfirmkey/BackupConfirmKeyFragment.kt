package io.horizontalsystems.bankwallet.modules.backupconfirmkey

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.helpers.KeyboardHelper
import kotlinx.android.synthetic.main.fragment_backup_words_confirm.*

class BackupConfirmKeyFragment : BaseFragment() {
    private val viewModel by viewModels<BackupConfirmKeyViewModel> { BackupConfirmKeyModule.Factory(arguments?.getParcelable(BackupConfirmKeyModule.ACCOUNT)!!) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_words_confirm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

        textDescription.setText(R.string.BackupConfirmKey_Description)

        viewModel.indexViewItemLiveData.observe(viewLifecycleOwner, { indexViewItem ->
            wordOne.bindPrefix(indexViewItem.first)
            wordTwo.bindPrefix(indexViewItem.second)
        })

        viewModel.errorLiveEvent.observe(viewLifecycleOwner, { error ->
            HudHelper.showErrorMessage(this.requireView(), error)
        })

        viewModel.successLiveEvent.observe(viewLifecycleOwner, {
            HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Success)
            Handler(Looper.getMainLooper()).postDelayed({
                findNavController().popBackStack(R.id.backupKeyFragment, true)
            }, 1200)
        })

        KeyboardHelper.showKeyboardDelayed(requireActivity(), wordOne, 200)

        viewModel.onViewCreated()
    }

    private fun onClickDone() {
        val firstWord = wordOne.getEnteredText() ?: ""
        val secondWord = wordTwo.getEnteredText() ?: ""
        viewModel.onClickDone(firstWord, secondWord)
    }

}

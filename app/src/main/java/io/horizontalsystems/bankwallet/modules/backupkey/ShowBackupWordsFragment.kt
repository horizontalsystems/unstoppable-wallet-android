package io.horizontalsystems.bankwallet.modules.backupkey

import android.os.Bundle
import android.view.View
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.backupconfirmkey.BackupConfirmKeyModule
import io.horizontalsystems.bankwallet.modules.showkey.ShowWordsFragment

class ShowBackupWordsFragment : ShowWordsFragment() {
    private val viewModel by navGraphViewModels<BackupKeyViewModel>(R.id.backupKeyFragment)

    override val actionButtonText: Int
        get() = R.string.BackupKey_ButtonBackup


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.openConfirmationLiveEvent.observe(viewLifecycleOwner, { account ->
            BackupConfirmKeyModule.start(this, R.id.showBackupWordsFragment_to_backupConfirmationKeyFragment, navOptions(), account)
        })
    }

    override fun onActionButtonClick() {
        viewModel.onClickBackup()
    }

    companion object {
        const val WORDS = ShowWordsFragment.WORDS
    }

}

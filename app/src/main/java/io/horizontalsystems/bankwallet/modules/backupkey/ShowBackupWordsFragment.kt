package io.horizontalsystems.bankwallet.modules.backupkey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.backupconfirmkey.BackupConfirmKeyModule
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_show_backup_words.*

class ShowBackupWordsFragment : BaseFragment() {
    private val viewModel by navGraphViewModels<BackupKeyViewModel>(R.id.backupKeyFragment)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_show_backup_words, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.openConfirmationLiveEvent.observe(viewLifecycleOwner, { account ->
            BackupConfirmKeyModule.start(this, R.id.showBackupWordsFragment_to_backupConfirmationKeyFragment, navOptions(), account)
        })

        buttonBackup.setOnClickListener {
            viewModel.onClickBackup()
        }

        mnemonicPhraseView.populateWords(viewModel.words, viewModel.passphrase)
    }

}

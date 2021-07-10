package io.horizontalsystems.bankwallet.modules.backupkey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.Toolbar
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.backupconfirmkey.BackupConfirmKeyModule
import io.horizontalsystems.bankwallet.ui.extensions.MnemonicPhraseView
import io.horizontalsystems.core.findNavController

class ShowBackupWordsFragment : BaseFragment() {
    private val viewModel by navGraphViewModels<BackupKeyViewModel>(R.id.backupKeyFragment)
    private lateinit var toolbar: Toolbar
    private lateinit var buttonBackup: Button
    private lateinit var mnemonicPhraseView: MnemonicPhraseView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        disallowScreenshot()
        return inflater.inflate(R.layout.fragment_show_backup_words, container, false)
    }

    override fun onDestroyView() {
        allowScreenshot()
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.toolbar)
        buttonBackup = view.findViewById(R.id.buttonBackup)
        mnemonicPhraseView = view.findViewById(R.id.mnemonicPhraseView)

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

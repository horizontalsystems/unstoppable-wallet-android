package io.horizontalsystems.bankwallet.modules.backupkey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.databinding.FragmentShowBackupWordsBinding
import io.horizontalsystems.bankwallet.modules.backupconfirmkey.BackupConfirmKeyModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.findNavController

class ShowBackupWordsFragment : BaseFragment() {
    private val viewModel by navGraphViewModels<BackupKeyViewModel>(R.id.backupKeyFragment)

    private var _binding: FragmentShowBackupWordsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShowBackupWordsBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        allowScreenshot()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.openConfirmationLiveEvent.observe(viewLifecycleOwner, { account ->
            BackupConfirmKeyModule.start(
                this,
                R.id.showBackupWordsFragment_to_backupConfirmationKeyFragment,
                navOptions(),
                account
            )
        })

        binding.mnemonicPhraseView.populateWords(viewModel.words, viewModel.passphrase)

        binding.buttonBackupCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        binding.buttonBackupCompose.setContent {
            ComposeAppTheme {
                ButtonPrimaryYellow(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 38.dp),
                    title = getString(R.string.BackupKey_ButtonBackup),
                    onClick = {
                        viewModel.onClickBackup()
                    }
                )
            }
        }
    }

}

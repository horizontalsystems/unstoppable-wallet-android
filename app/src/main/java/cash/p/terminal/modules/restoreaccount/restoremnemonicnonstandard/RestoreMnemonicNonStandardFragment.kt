package cash.p.terminal.modules.restoreaccount.restoremnemonicnonstandard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.modules.manageaccounts.ManageAccountsModule
import cash.p.terminal.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController

class RestoreMnemonicNonStandardFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    val popUpToInclusiveId =
                        arguments?.getInt(ManageAccountsModule.popOffOnSuccessKey, R.id.restoreAccountAdvancedFragment) ?: R.id.restoreAccountAdvancedFragment

                    RestorePhraseNonStandard(findNavController(), popUpToInclusiveId)
                }
            }
        }
    }
}

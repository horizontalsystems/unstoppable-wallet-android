package cash.p.terminal.modules.restoreaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.modules.manageaccounts.ManageAccountsModule
import cash.p.terminal.modules.restoreaccount.restoremenu.RestoreMenuModule
import cash.p.terminal.modules.restoreaccount.restoremenu.RestoreMenuViewModel
import cash.p.terminal.modules.restoreaccount.restoremnemonic.RestorePhrase
import cash.p.terminal.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController

class RestoreAccountFragment : BaseFragment() {

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
                val popUpToInclusiveId =
                    arguments?.getInt(ManageAccountsModule.popOffOnSuccessKey, R.id.restoreAccountFragment) ?: R.id.restoreAccountFragment

                RestoreScreen(findNavController(), popUpToInclusiveId)
            }
        }
    }
}

@Composable
fun RestoreScreen(
    navController: NavController,
    popUpToInclusiveId: Int,
    restoreMenuViewModel: RestoreMenuViewModel = viewModel(factory = RestoreMenuModule.Factory())
) {
    ComposeAppTheme {
        RestorePhrase(navController, popUpToInclusiveId, restoreMenuViewModel, advanced = false)
    }
}

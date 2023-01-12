package io.horizontalsystems.bankwallet.modules.restoreaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.restoreaccount.resoreprivatekey.RestorePrivateKey
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremenu.RestoreMenuModule
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremenu.RestoreMenuModule.RestoreOption
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremenu.RestoreMenuViewModel
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonic.RestorePhrase
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController

class AdvancedRestoreFragment : BaseFragment() {

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
                        arguments?.getInt(ManageAccountsModule.popOffOnSuccessKey, R.id.advancedRestoreFragment) ?: R.id.advancedRestoreFragment

                    AdvancedRestoreScreen(findNavController(), popUpToInclusiveId)
                }
            }
        }
    }
}

@Composable
fun AdvancedRestoreScreen(
    navController: NavController,
    popUpToInclusiveId: Int,
    restoreMenuViewModel: RestoreMenuViewModel = viewModel(factory = RestoreMenuModule.Factory())
) {
    when (restoreMenuViewModel.restoreOption) {
        RestoreOption.RecoveryPhrase -> {
            RestorePhrase(navController, popUpToInclusiveId, restoreMenuViewModel)
        }
        RestoreOption.PrivateKey -> {
            RestorePrivateKey(navController, popUpToInclusiveId, restoreMenuViewModel)
        }
    }
}

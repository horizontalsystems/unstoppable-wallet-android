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
import io.horizontalsystems.bankwallet.modules.restoreaccount.resoreprivatekey.ResorePrivateKey
import io.horizontalsystems.bankwallet.modules.restoreaccount.restore.RestoreModule
import io.horizontalsystems.bankwallet.modules.restoreaccount.restore.RestoreModule.RestoreOption
import io.horizontalsystems.bankwallet.modules.restoreaccount.restore.RestoreViewModel
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonic.RestorePhrase
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController

class RestoreMnemonicFragment : BaseFragment() {

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
                    val popUpToInclusiveId = arguments?.getInt(
                        ManageAccountsModule.popOffOnSuccessKey, R.id.restoreMnemonicFragment
                    ) ?: R.id.restoreMnemonicFragment

                    RestoreScreen(findNavController(), popUpToInclusiveId)
                }
            }
        }
    }
}

@Composable
fun RestoreScreen(
    navController: NavController,
    popUpToInclusiveId: Int,
    restoreViewModel: RestoreViewModel = viewModel(factory = RestoreModule.Factory())
) {
    when (restoreViewModel.restoreOption) {
        RestoreOption.RecoveryPhrase -> {
            RestorePhrase(navController, popUpToInclusiveId, restoreViewModel)
        }
        RestoreOption.PrivateKey -> {
            ResorePrivateKey(navController, popUpToInclusiveId, restoreViewModel)
        }
    }
}

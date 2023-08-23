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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.core.composablePopup
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.ManageWalletsScreen
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremenu.RestoreMenuModule
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremenu.RestoreMenuViewModel
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonic.RestorePhrase
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonicnonstandard.RestorePhraseNonStandard
import io.horizontalsystems.bankwallet.modules.zcashconfigure.ZcashConfigureScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
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

                val inclusive =
                    arguments?.getBoolean(ManageAccountsModule.popOffInclusiveKey) ?: false

                ComposeAppTheme {
                    RestoreAccountNavHost(findNavController(), popUpToInclusiveId, inclusive)
                }
            }
        }
    }
}

@Composable
private fun RestoreAccountNavHost(
    fragmentNavController: NavController,
    popUpToInclusiveId: Int,
    inclusive: Boolean
) {
    val navController = rememberNavController()
    val restoreMenuViewModel: RestoreMenuViewModel = viewModel(factory = RestoreMenuModule.Factory())
    val mainViewModel: RestoreViewModel = viewModel()
    NavHost(
        navController = navController,
        startDestination = "restore_phrase",
    ) {
        composable("restore_phrase") {
            RestorePhrase(
                advanced = false,
                restoreMenuViewModel = restoreMenuViewModel,
                mainViewModel = mainViewModel,
                openRestoreAdvanced = { navController.navigate("restore_phrase_advanced") },
                openSelectCoins = { navController.navigate("restore_select_coins") },
                openNonStandardRestore = { navController.navigate("restore_phrase_nonstandard") },
                onBackClick = { fragmentNavController.popBackStack() },
            )
        }
        composablePage("restore_phrase_advanced") {
            AdvancedRestoreScreen(
                restoreMenuViewModel = restoreMenuViewModel,
                mainViewModel = mainViewModel,
                openSelectCoinsScreen = { navController.navigate("restore_select_coins") },
                openNonStandardRestore = { navController.navigate("restore_phrase_nonstandard") },
                onBackClick = { navController.popBackStack() }
            )
        }
        composablePage("restore_select_coins") {
            ManageWalletsScreen(
                mainViewModel = mainViewModel,
                openZCashConfigure = { navController.navigate("zcash_configure") },
                onBackClick = { navController.popBackStack() }
            ) { fragmentNavController.popBackStack(popUpToInclusiveId, inclusive) }
        }
        composablePage("restore_phrase_nonstandard") {
            RestorePhraseNonStandard(
                mainViewModel = mainViewModel,
                openSelectCoinsScreen = { navController.navigate("restore_select_coins") },
                onBackClick = { navController.popBackStack() }
            )
        }
        composablePopup("zcash_configure") {
            ZcashConfigureScreen(
                onCloseWithResult = { config ->
                    mainViewModel.setZCashConfig(config)
                    navController.popBackStack()
                },
                onCloseClick = {
                    mainViewModel.cancelZCashConfig = true
                    navController.popBackStack()
                }
            )
        }
    }
}

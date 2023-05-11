package cash.p.terminal.modules.restoreaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.composablePage
import cash.p.terminal.core.composablePopup
import cash.p.terminal.modules.manageaccounts.ManageAccountsModule
import cash.p.terminal.modules.restoreaccount.restoreblockchains.ManageWalletsScreen
import cash.p.terminal.modules.restoreaccount.restoremenu.RestoreMenuModule
import cash.p.terminal.modules.restoreaccount.restoremenu.RestoreMenuViewModel
import cash.p.terminal.modules.restoreaccount.restoremnemonic.RestorePhrase
import cash.p.terminal.modules.restoreaccount.restoremnemonicnonstandard.RestorePhraseNonStandard
import cash.p.terminal.modules.zcashconfigure.ZcashConfigureScreen
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

                ComposeAppTheme {
                    RestoreAccountNavHost(findNavController(), popUpToInclusiveId)
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun RestoreAccountNavHost(fragmentNavController: NavController, popUpToInclusiveId: Int) {
    val navController = rememberAnimatedNavController()
    val restoreMenuViewModel: RestoreMenuViewModel = viewModel(factory = RestoreMenuModule.Factory())
    val mainViewModel: RestoreViewModel = viewModel()
    AnimatedNavHost(
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
            ) { fragmentNavController.popBackStack(popUpToInclusiveId, true) }
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

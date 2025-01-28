package cash.p.terminal.modules.restoreaccount

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cash.p.terminal.R
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.core.composablePage
import cash.p.terminal.core.composablePopup
import io.horizontalsystems.core.getInput
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.stat
import cash.p.terminal.modules.manageaccounts.ManageAccountsModule
import cash.p.terminal.modules.restoreaccount.restoreblockchains.ManageWalletsScreen
import cash.p.terminal.modules.restoreaccount.restoremenu.RestoreMenuModule
import cash.p.terminal.modules.restoreaccount.restoremenu.RestoreMenuViewModel
import cash.p.terminal.modules.restoreaccount.restoremnemonic.RestorePhrase
import cash.p.terminal.modules.restoreaccount.restoremnemonicnonstandard.RestorePhraseNonStandard
import cash.p.terminal.modules.zcashconfigure.ZcashConfigureScreen

class RestoreAccountFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<ManageAccountsModule.Input>()
        val popUpToInclusiveId = input?.popOffOnSuccess ?: R.id.restoreAccountFragment
        val inclusive = input?.popOffInclusive ?: false

        RestoreAccountNavHost(
            navController,
            popUpToInclusiveId,
            inclusive
        )
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
                openNonStandardRestore = {
                    navController.navigate("restore_phrase_nonstandard")

                    stat(page = StatPage.ImportWalletFromKeyAdvanced, event = StatEvent.Open(StatPage.ImportWalletNonStandard))
                },
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

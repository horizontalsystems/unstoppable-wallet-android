package io.horizontalsystems.bankwallet.modules.restoreaccount

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.ManageWalletsScreen
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremenu.RestoreMenuModule
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremenu.RestoreMenuViewModel
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonic.RestorePhrase
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonicnonstandard.RestorePhraseNonStandard
import io.horizontalsystems.bankwallet.modules.restoreconfig.RestoreBirthdayHeightScreen
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.delay

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
    val restoreMenuViewModel: RestoreMenuViewModel =
        viewModel(factory = RestoreMenuModule.Factory())
    val mainViewModel: RestoreViewModel = viewModel()

    val view = LocalView.current
    val onFinish: () -> Unit = {
        fragmentNavController.popBackStack(popUpToInclusiveId, inclusive)
    }

    val uiState = mainViewModel.uiState

    LaunchedEffect(uiState.openSelectCoinsScreen) {
        if (uiState.openSelectCoinsScreen) {
            mainViewModel.openSelectCoinsScreenHandled()
            navController.navigate("restore_select_coins")
        }
    }

    LaunchedEffect(uiState.restored) {
        if (uiState.restored) {
            HudHelper.showSuccessMessage(
                contenView = view,
                resId = R.string.Hud_Text_Restored,
                icon = R.drawable.icon_add_to_wallet_2_24,
                iconTint = R.color.white
            )
            delay(300)
            onFinish.invoke()
        }
    }

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
                openSelectCoins = { mainViewModel.requestOpenSelectCoinsScreen() },
                openNonStandardRestore = { navController.navigate("restore_phrase_nonstandard") },
                onBackClick = { fragmentNavController.popBackStack() },
            )
        }
        composablePage("restore_phrase_advanced") {
            AdvancedRestoreScreen(
                restoreMenuViewModel = restoreMenuViewModel,
                mainViewModel = mainViewModel,
                openSelectNetworkScreen = { navController.navigate("restore_select_network") },
                openSelectCoinsScreen = { mainViewModel.requestOpenSelectCoinsScreen() },
                openNonStandardRestore = {
                    navController.navigate("restore_phrase_nonstandard")

                    stat(
                        page = StatPage.ImportWalletFromKeyAdvanced,
                        event = StatEvent.Open(StatPage.ImportWalletNonStandard)
                    )
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composablePage("restore_select_network") {
            SelectNetworkScreen(
                mainViewModel = mainViewModel,
                openSelectCoinsScreen = { mainViewModel.requestOpenSelectCoinsScreen() },
                onBackClick = { navController.popBackStack() }
            )
        }
        composablePage("restore_select_coins") {
            ManageWalletsScreen(
                mainViewModel = mainViewModel,
                openBirthdayHeightConfigure = { token ->
                    when (token.blockchainType) {
                        BlockchainType.Zcash -> navController.navigate("zcash_configure")
                        BlockchainType.Monero -> navController.navigate("monero_configure")
                        else -> Unit
                    }
                },
                onBackClick = { navController.popBackStack() },
                onFinish = onFinish
            )
        }
        composablePage("restore_phrase_nonstandard") {
            RestorePhraseNonStandard(
                mainViewModel = mainViewModel,
                openSelectCoinsScreen = { mainViewModel.requestOpenSelectCoinsScreen() },
                onBackClick = { navController.popBackStack() }
            )
        }
        composablePage("zcash_configure") {
            RestoreBirthdayHeightScreen(
                blockchainType = BlockchainType.Zcash,
                onCloseWithResult = { config ->
                    mainViewModel.setBirthdayHeightConfig(config)
                    navController.popBackStack()
                },
                onCloseClick = {
                    mainViewModel.cancelBirthdayHeightConfig = true
                    navController.popBackStack()
                }
            )
        }
        composablePage("monero_configure") {
            RestoreBirthdayHeightScreen(
                blockchainType = BlockchainType.Monero,
                onCloseWithResult = { config ->
                    mainViewModel.setBirthdayHeightConfig(config)
                    navController.popBackStack()
                },
                onCloseClick = {
                    mainViewModel.cancelBirthdayHeightConfig = true
                    navController.popBackStack()
                }
            )
        }
    }
}

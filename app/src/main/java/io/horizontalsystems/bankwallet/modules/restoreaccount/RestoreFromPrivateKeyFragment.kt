package io.horizontalsystems.bankwallet.modules.restoreaccount

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.removeLastUntil
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.ManageWalletsScreen
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreprivatekey.RestorePrivateKey
import io.horizontalsystems.bankwallet.modules.restoreconfig.RestoreBirthdayHeightScreen
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.delay
import kotlin.reflect.KClass

class RestoreFromPrivateKeyFragment(val input: ManageAccountsModule.Input?) : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        val popUpToInclusiveId = input?.popOffOnSuccess ?: RestoreFromPrivateKeyFragment::class
        val inclusive = input?.popOffInclusive ?: false

        RestoreFromPrivateKeyNavHost(navController, popUpToInclusiveId, inclusive)
    }
}

@Composable
private fun RestoreFromPrivateKeyNavHost(
    fragmentNavController: NavBackStack<HSScreen>,
    popUpToInclusiveId: KClass<out HSScreen>,
    inclusive: Boolean,
) {
    val navController = rememberNavController()
    val mainViewModel: RestoreViewModel = viewModel()

    val view = LocalView.current
    val onFinish: () -> Unit = {
        fragmentNavController.removeLastUntil(popUpToInclusiveId, inclusive)
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
        startDestination = "restore_private_key",
    ) {
        composablePage("restore_private_key") {
            RestorePrivateKey(
                mainViewModel = mainViewModel,
                openSelectNetworkScreen = { navController.navigate("restore_select_network") },
                openSelectCoinsScreen = { mainViewModel.requestOpenSelectCoinsScreen() },
                onBackClick = { fragmentNavController.removeLastOrNull() },
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

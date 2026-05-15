package io.horizontalsystems.bankwallet.modules.restoreaccount

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.ManageWalletsScreen
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonic.RestorePhrase
import io.horizontalsystems.bankwallet.modules.restoreconfig.RestoreBirthdayHeightScreen
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
data class RestoreAccountFragment(val input: ManageAccountsModule.Input) : HSScreen(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        RestoreAccountNavHost(
            navController,
            input
        )
    }

}

@Composable
private fun RestoreAccountNavHost(
    navController: HSNavigation,
    input: ManageAccountsModule.Input
) {
    val mainViewModel: RestoreViewModel = viewModel()

    val view = LocalView.current

    val uiState = mainViewModel.uiState

    LaunchedEffect(uiState.openSelectCoinsScreen) {
        if (uiState.openSelectCoinsScreen) {
            mainViewModel.openSelectCoinsScreenHandled()
            navController.add(restore_select_coins(input))
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
            navController.removeLastUntil(input.popOffOnSuccess, input.popOffInclusive)
        }
    }

    RestorePhrase(
        mainViewModel = mainViewModel,
        openSelectCoins = { mainViewModel.requestOpenSelectCoinsScreen() },
        onBackClick = { navController.removeLastOrNull() },
    )
}

@Serializable
data class restore_select_coins(val input: ManageAccountsModule.Input) : HSScreen() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val mainViewModel = navController.viewModelForScreen<RestoreViewModel>(RestoreAccountFragment::class)

        ManageWalletsScreen(
            mainViewModel = mainViewModel,
            openBirthdayHeightConfigure = { token ->
                when (token.blockchainType) {
                    BlockchainType.Zcash -> navController.add(zcash_configure)
                    BlockchainType.Monero -> navController.add(monero_configure)
                    BlockchainType.Zano -> navController.add(zano_configure)
                    else -> Unit
                }
            },
            onBackClick = { navController.removeLastOrNull() },
            onFinish = {
                navController.removeLastUntil(input.popOffOnSuccess, input.popOffInclusive)
            }
        )
    }
}

@Serializable
data object zcash_configure : HSScreen() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val mainViewModel = navController.viewModelForScreen<RestoreViewModel>(RestoreAccountFragment::class)

        RestoreBirthdayHeightScreen(
            blockchainType = BlockchainType.Zcash,
            onCloseWithResult = { config ->
                mainViewModel.setBirthdayHeightConfig(config)
                navController.removeLastOrNull()
            },
            onCloseClick = {
                mainViewModel.cancelBirthdayHeightConfig = true
                navController.removeLastOrNull()
            }
        )
    }
}

@Serializable
data object monero_configure : HSScreen() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val mainViewModel = navController.viewModelForScreen<RestoreViewModel>(RestoreAccountFragment::class)

        RestoreBirthdayHeightScreen(
            blockchainType = BlockchainType.Monero,
            onCloseWithResult = { config ->
                mainViewModel.setBirthdayHeightConfig(config)
                navController.removeLastOrNull()
            },
            onCloseClick = {
                mainViewModel.cancelBirthdayHeightConfig = true
                navController.removeLastOrNull()
            }
        )
    }
}

@Serializable
data object zano_configure : HSScreen() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val mainViewModel = navController.viewModelForScreen<RestoreViewModel>(RestoreAccountFragment::class)

        RestoreBirthdayHeightScreen(
            blockchainType = BlockchainType.Zano,
            onCloseWithResult = { config ->
                mainViewModel.setBirthdayHeightConfig(config)
                navController.removeLastOrNull()
            },
            onCloseClick = {
                mainViewModel.cancelBirthdayHeightConfig = true
                navController.removeLastOrNull()
            }
        )
    }
}
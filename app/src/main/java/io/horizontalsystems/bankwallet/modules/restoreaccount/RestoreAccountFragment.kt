package io.horizontalsystems.bankwallet.modules.restoreaccount

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.ManageWalletsScreen
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonic.RestorePhrase
import io.horizontalsystems.bankwallet.modules.restoreconfig.RestoreBirthdayHeightScreen
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.serialization.Serializable

@Serializable
data class RestoreAccountFragment(val input: ManageAccountsModule.Input) : HSScreen(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        RestorePhrase(
            openSelectCoins = { accountType: AccountType, accountName: String, manualBackup: Boolean, fileBackup: Boolean, statPage: StatPage ->
                navController.add(
                    restore_select_coins(
                        input,
                        accountType,
                        accountName,
                        manualBackup,
                        fileBackup,
                        statPage
                    )
                )
            },
            onBackClick = { navController.removeLastOrNull() },
        )
    }
}

@Serializable
data class restore_select_coins(
    val input: ManageAccountsModule.Input,
    val accountType: AccountType,
    val accountName: String,
    val manualBackup: Boolean,
    val fileBackup: Boolean,
    val statPage: StatPage
) : HSScreen() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val mainViewModel = viewModel<RestoreViewModel> {
            RestoreViewModel(
                accountType,
                accountName,
                manualBackup,
                fileBackup,
                statPage
            )
        }

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
        val mainViewModel = navController.viewModelForScreen<RestoreViewModel>(restore_select_coins::class)

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
        val mainViewModel = navController.viewModelForScreen<RestoreViewModel>(restore_select_coins::class)

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
        val mainViewModel = navController.viewModelForScreen<RestoreViewModel>(restore_select_coins::class)

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
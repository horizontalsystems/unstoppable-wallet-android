package io.horizontalsystems.bankwallet.modules.restoreaccount

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.BirthdayHeightConfig
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.bankwallet.modules.nav3.ResultEffect
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.ManageWalletsScreen
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonic.RestorePhrase
import io.horizontalsystems.bankwallet.modules.restoreconfig.RestoreBirthdayHeightScreen
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class RestoreAccountPage(val input: ManageAccountsModule.Input) : HSPage(screenshotEnabled = false) {

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
) : HSPage() {
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

        val uuid = rememberSaveable { UUID.randomUUID().toString() }
        ResultEffect<RestoreBirthdayHeightPage.Result>(resultKeyUuid = uuid) {
            val config = it.config
            if (config != null) {
                mainViewModel.setBirthdayHeightConfig2(config)
            } else {
                mainViewModel.cancelBirthdayHeightConfig = true
            }
        }

        ManageWalletsScreen(
            mainViewModel = mainViewModel,
            openBirthdayHeightConfigure = { token ->
                val screen = RestoreBirthdayHeightPage(token.blockchainType)
                screen.resultKey = uuid
                navController.add(screen)
            },
            onBackClick = { navController.removeLastOrNull() },
            onFinish = {
                navController.removeLastUntil(input.popOffOnSuccess, input.popOffInclusive)
            }
        )
    }
}

@Serializable
data class RestoreBirthdayHeightPage(val blockchainType: BlockchainType) : HSPage() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val resultEventBus = LocalResultEventBus.current
        RestoreBirthdayHeightScreen(
            blockchainType = blockchainType,
            onCloseWithResult = { config ->
                resultEventBus.sendResult(Result(config))
                navController.removeLastOrNull()
            },
            onCloseClick = {
                resultEventBus.sendResult(Result(null))
                navController.removeLastOrNull()
            }
        )
    }

    data class Result(val config: BirthdayHeightConfig?)
}

package io.horizontalsystems.bankwallet.modules.restoreaccount

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEffect
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.ManageWalletsScreen
import io.horizontalsystems.bankwallet.serializers.HSScreenKClassSerializer
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.reflect.KClass

@Serializable
data class RestoreFromPasskeyFragment(val input: Input) : HSScreen(screenshotEnabled = false) {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        RestoreFromPasskeyNavHost(navController, input)
    }

    @Serializable
    data class Input(
        @Serializable(with = HSScreenKClassSerializer::class) val popOffOnSuccess: KClass<out HSScreen>,
        val popOffInclusive: Boolean,
        val entropy: ByteArray,
        val accountName: String?
    )
}

@Composable
private fun RestoreFromPasskeyNavHost(
    navController: HSNavigation,
    input: RestoreFromPasskeyFragment.Input,
) {
    val viewModel = viewModel<RestoreFromPasskeyViewModel>(
        factory = RestoreFromPasskeyViewModel.Factory()
    )

    val mainViewModel: RestoreViewModel = viewModel {
        val accountType = viewModel.getAccountType(input.entropy)
        val accountName = viewModel.getAccountName(input.accountName)

        RestoreViewModel(
            accountType = accountType,
            accountName = accountName,
            manualBackup = true,
            fileBackup = false,
            statPage = StatPage.ImportWalletFromPasskey
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

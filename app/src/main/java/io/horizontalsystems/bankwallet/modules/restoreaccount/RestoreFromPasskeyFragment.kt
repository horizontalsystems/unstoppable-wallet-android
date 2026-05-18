package io.horizontalsystems.bankwallet.modules.restoreaccount

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.serializers.HSScreenKClassSerializer
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class RestoreFromPasskeyFragment(val input: Input) : HSScreen(screenshotEnabled = false) {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val viewModel = viewModel<RestoreFromPasskeyViewModel>(
            factory = RestoreFromPasskeyViewModel.Factory()
        )

        val accountType = remember { viewModel.getAccountType(input.entropy) }
        val accountName = remember { viewModel.getAccountName(input.accountName) }

        restore_select_coins(
            input = ManageAccountsModule.Input(input.popOffOnSuccess, input.popOffInclusive),
            accountType = accountType,
            accountName = accountName,
            manualBackup = true,
            fileBackup = false,
            statPage = StatPage.ImportWalletFromPasskey
        ).GetContent(navController)
    }

    @Serializable
    data class Input(
        @Serializable(with = HSScreenKClassSerializer::class) val popOffOnSuccess: KClass<out HSScreen>,
        val popOffInclusive: Boolean,
        val entropy: ByteArray,
        val accountName: String?
    )
}

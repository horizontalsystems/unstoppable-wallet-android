package io.horizontalsystems.walletkit.modules.restoreaccount

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.serializers.HSScreenKClassSerializer
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class RestoreFromPasskeyPage(val input: Input) : HSPage(screenshotEnabled = false) {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
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
        ).GetContent(navigation)
    }

    @Serializable
    data class Input(
        @Serializable(with = HSScreenKClassSerializer::class) val popOffOnSuccess: KClass<out HSPage>,
        val popOffInclusive: Boolean,
        val entropy: ByteArray,
        val accountName: String?
    )
}

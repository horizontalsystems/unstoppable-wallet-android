package io.horizontalsystems.bankwallet.modules.transactionInfo.options

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.nav3.viewModelForScreen
import kotlinx.serialization.Serializable

@Serializable
data object TransactionSpeedUpCancelTransactionSettingsPage : HSPage() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        TransactionSpeedUpCancelTransactionSettingsScreen(navController)
    }
}

@Composable
fun TransactionSpeedUpCancelTransactionSettingsScreen(navController: HSNavigation) {
    val viewModel = viewModelForScreen<TransactionSpeedUpCancelViewModel>(
        TransactionSpeedUpCancelPage::class
    )

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(navController)
}

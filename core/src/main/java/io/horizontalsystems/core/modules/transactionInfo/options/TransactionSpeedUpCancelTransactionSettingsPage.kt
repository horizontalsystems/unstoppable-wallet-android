package io.horizontalsystems.core.modules.transactionInfo.options

import androidx.compose.runtime.Composable
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.nav3.HSPage
import kotlinx.serialization.Serializable

@Serializable
data object TransactionSpeedUpCancelTransactionSettingsPage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        TransactionSpeedUpCancelTransactionSettingsScreen(navigation)
    }
}

@Composable
fun TransactionSpeedUpCancelTransactionSettingsScreen(navigation: HSNavigation) {
    val viewModel = navigation.viewModelForScreen<TransactionSpeedUpCancelViewModel>(TransactionSpeedUpCancelPage::class)

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(navigation)
}

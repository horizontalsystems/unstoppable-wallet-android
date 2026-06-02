package io.horizontalsystems.bankwallet.modules.eip20revoke

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.nav3.viewModelForScreen
import kotlinx.serialization.Serializable

@Serializable
data object Eip20RevokeTransactionSettingsPage : HSPage() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        Eip20RevokeTransactionSettingsScreen(navController)
    }
}

@Composable
fun Eip20RevokeTransactionSettingsScreen(navController: HSNavigation) {
    val viewModel = viewModelForScreen<Eip20RevokeConfirmViewModel>(Eip20RevokeConfirmPage::class)

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(navController)
}

package io.horizontalsystems.walletkit.modules.eip20revoke

import androidx.compose.runtime.Composable
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import kotlinx.serialization.Serializable

@Serializable
data object Eip20RevokeTransactionSettingsPage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        Eip20RevokeTransactionSettingsScreen(navigation)
    }
}

@Composable
fun Eip20RevokeTransactionSettingsScreen(navigation: HSNavigation) {
    val viewModel = navigation.viewModelForScreen<Eip20RevokeConfirmViewModel>(Eip20RevokeConfirmPage::class)

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(navigation)
}

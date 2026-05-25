package io.horizontalsystems.bankwallet.modules.eip20approve

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import kotlinx.serialization.Serializable

@Serializable
data object Eip20ApproveTransactionSettingsPage : HSPage() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        Eip20ApproveTransactionSettingsScreen(navController)
    }
}

@Composable
fun Eip20ApproveTransactionSettingsScreen(navController: HSNavigation) {
    val viewModel = navController.viewModelForScreen<Eip20ApproveViewModel>(Eip20ApprovePage::class)

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(navController)
}

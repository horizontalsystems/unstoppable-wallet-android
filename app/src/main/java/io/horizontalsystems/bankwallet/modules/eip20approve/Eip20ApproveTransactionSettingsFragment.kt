package io.horizontalsystems.bankwallet.modules.eip20approve

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.serialization.Serializable

@Serializable
data object Eip20ApproveTransactionSettingsFragment : HSScreen() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        Eip20ApproveTransactionSettingsScreen(navController)
    }
}

@Composable
fun Eip20ApproveTransactionSettingsScreen(navController: HSNavigation) {
    val viewModel = navController.viewModelForScreen<Eip20ApproveViewModel>(Eip20ApproveFragment::class)

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(navController)
}

package io.horizontalsystems.bankwallet.modules.eip20revoke

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.serialization.Serializable

@Serializable
data object Eip20RevokeTransactionSettingsFragment : HSScreen() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        Eip20RevokeTransactionSettingsScreen(navController)
    }
}

@Composable
fun Eip20RevokeTransactionSettingsScreen(navController: HSNavigation) {
    val viewModel = navController.viewModelForScreen<Eip20RevokeConfirmViewModel>(Eip20RevokeConfirmFragment::class)

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(navController)
}

package io.horizontalsystems.bankwallet.modules.eip20revoke

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.viewModelForScreen

class Eip20RevokeTransactionSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        Eip20RevokeTransactionSettingsScreen(navController)
    }
}

@Composable
fun Eip20RevokeTransactionSettingsScreen(navController: NavBackStack<HSScreen>) {
    val viewModel = navController.viewModelForScreen<Eip20RevokeConfirmViewModel>(Eip20RevokeConfirmFragment::class)

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(navController)
}

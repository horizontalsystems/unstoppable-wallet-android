package io.horizontalsystems.bankwallet.modules.send.evm.settings

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.viewModelForScreen
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.SendEvmConfirmationFragment
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.SendEvmConfirmationViewModel

class SendEvmNonceSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        NonceSettingsScreen(navController)
    }
}

@Composable
fun NonceSettingsScreen(navController: NavBackStack<HSScreen>) {
    val viewModel = navController.viewModelForScreen<SendEvmConfirmationViewModel>(SendEvmConfirmationFragment::class)
    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetNonceSettingsContent(navController)
}

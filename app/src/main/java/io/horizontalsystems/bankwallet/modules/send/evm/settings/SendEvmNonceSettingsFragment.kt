package io.horizontalsystems.bankwallet.modules.send.evm.settings

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.SendEvmConfirmationScreen
import io.horizontalsystems.bankwallet.modules.send.evm.confirmation.SendEvmConfirmationViewModel
import kotlinx.serialization.Serializable

@Serializable
data object SendEvmNonceSettingsScreen : HSScreen(
    parentScreenClass = SendEvmConfirmationScreen::class
) {
    @Composable
    override fun GetContent(backStack: NavBackStack<HSScreen>) {
        NonceSettingsScreen(backStack)
    }
}

class SendEvmNonceSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
//        NonceSettingsScreen(navController)
    }
}

@Composable
fun NonceSettingsScreen(backStack: NavBackStack<HSScreen>) {
    val viewModel = viewModel<SendEvmConfirmationViewModel>()

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetNonceSettingsContent(backStack)
}

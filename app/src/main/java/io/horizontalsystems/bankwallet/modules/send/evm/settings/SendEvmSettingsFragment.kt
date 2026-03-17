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
data object SendEvmSettingsScreen : HSScreen(
    parentScreenClass = SendEvmConfirmationScreen::class
) {
    @Composable
    override fun GetContent(backStack: NavBackStack<HSScreen>) {
        SendEvmSettingsScreen(backStack)
    }
}

class SendEvmSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
//        SendEvmSettingsScreen(navController)
    }
}

@Composable
fun SendEvmSettingsScreen(backStack: NavBackStack<HSScreen>) {
    val viewModel = viewModel<SendEvmConfirmationViewModel>()

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(backStack)
}

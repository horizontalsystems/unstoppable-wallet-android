package io.horizontalsystems.bankwallet.modules.walletconnect.request

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WCSendEthereumTransactionRequestViewModel
import kotlinx.serialization.Serializable

@Serializable
data object WCEvmTransactionSettingsScreen : HSScreen(
    parentScreenClass = WCRequestScreen::class
) {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        WCEvmTransactionSettingsScreen(backStack)
    }
}

class WCEvmTransactionSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
    }
}

@Composable
fun WCEvmTransactionSettingsScreen(backStack: NavBackStack<HSScreen>) {
    val viewModel = viewModel<WCSendEthereumTransactionRequestViewModel>()

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(backStack)
}

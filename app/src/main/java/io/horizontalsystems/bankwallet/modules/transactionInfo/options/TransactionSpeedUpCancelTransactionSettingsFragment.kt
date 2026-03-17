package io.horizontalsystems.bankwallet.modules.transactionInfo.options

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.serialization.Serializable

@Serializable
data object TransactionSpeedUpCancelTransactionSettingsScreen : HSScreen(
    parentScreenClass = TransactionSpeedUpCancelScreen::class
) {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>
    ) {
        TransactionSpeedUpCancelTransactionSettingsScreen(backStack)
    }
}

class TransactionSpeedUpCancelTransactionSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
    }
}

@Composable
fun TransactionSpeedUpCancelTransactionSettingsScreen(backStack: NavBackStack<HSScreen>) {
    val viewModel = viewModel<TransactionSpeedUpCancelViewModel>()

    val sendTransactionService = viewModel.sendTransactionService
    sendTransactionService.GetSettingsContent(backStack)
}

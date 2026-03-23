package io.horizontalsystems.bankwallet.modules.transactionInfo.options

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.viewModelForScreen

class TransactionSpeedUpCancelTransactionSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        TransactionSpeedUpCancelTransactionSettingsScreen(navController)
    }
}

@Composable
fun TransactionSpeedUpCancelTransactionSettingsScreen(navController: NavBackStack<HSScreen>) {
    val viewModel = navController.viewModelForScreen<TransactionSpeedUpCancelViewModel>(TransactionSpeedUpCancelFragment::class)

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(navController)
}

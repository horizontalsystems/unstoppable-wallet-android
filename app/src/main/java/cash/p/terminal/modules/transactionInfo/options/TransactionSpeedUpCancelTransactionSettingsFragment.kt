package cash.p.terminal.modules.transactionInfo.options

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.ui_compose.BaseComposeFragment

class TransactionSpeedUpCancelTransactionSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        TransactionSpeedUpCancelTransactionSettingsScreen(navController)
    }
}

@Composable
fun TransactionSpeedUpCancelTransactionSettingsScreen(navController: NavController) {
    val viewModelStoreOwner = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(R.id.transactionSpeedUpCancelFragment)
    }

    val viewModel = viewModel<TransactionSpeedUpCancelViewModel>(
        viewModelStoreOwner = viewModelStoreOwner,
    )

    val sendTransactionService = viewModel.sendTransactionService

    sendTransactionService.GetSettingsContent(navController)
}

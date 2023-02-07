package cash.p.terminal.modules.balance.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.modules.balance.BalanceAccountsViewModel
import cash.p.terminal.modules.balance.BalanceModule
import cash.p.terminal.modules.balance.BalanceScreenState
import cash.p.terminal.ui.compose.ComposeAppTheme

@Composable
fun BalanceScreen(navController: NavController) {
    ComposeAppTheme {
        val viewModel = viewModel<BalanceAccountsViewModel>(factory = BalanceModule.AccountsFactory())

        when (val tmpAccount = viewModel.balanceScreenState) {
            BalanceScreenState.NoAccount -> BalanceNoAccount(navController)
            is BalanceScreenState.HasAccount -> BalanceForAccount(navController, tmpAccount.accountViewItem)
            else -> {}
        }
    }
}
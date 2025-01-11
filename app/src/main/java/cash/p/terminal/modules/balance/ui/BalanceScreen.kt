package cash.p.terminal.modules.balance.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.modules.balance.BalanceAccountsViewModel
import cash.p.terminal.modules.balance.BalanceModule
import cash.p.terminal.modules.balance.BalanceScreenState
import cash.p.terminal.modules.balance.cex.BalanceForAccountCex

@Composable
fun BalanceScreen(navController: NavController) {
    val viewModel = viewModel<BalanceAccountsViewModel>(factory = BalanceModule.AccountsFactory())

    when (val tmpAccount = viewModel.balanceScreenState) {
        BalanceScreenState.NoAccount -> BalanceNoAccount(navController)
        is BalanceScreenState.HasAccount -> when (tmpAccount.accountViewItem.type) {
            is AccountType.Cex -> {
                BalanceForAccountCex(navController, tmpAccount.accountViewItem)
            }

            else -> {
                BalanceForAccount(navController, tmpAccount.accountViewItem)
            }
        }

        else -> {}
    }
}
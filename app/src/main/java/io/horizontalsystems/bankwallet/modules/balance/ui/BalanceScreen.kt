package io.horizontalsystems.bankwallet.modules.balance.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.balance.BalanceAccountsViewModel
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule
import io.horizontalsystems.bankwallet.modules.balance.BalanceScreenState
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen

@Composable
fun BalanceScreen(
    backStack: NavBackStack<HSScreen>,
) {
    val viewModel = viewModel<BalanceAccountsViewModel>(factory = BalanceModule.AccountsFactory())

    when (val tmpAccount = viewModel.balanceScreenState) {
        BalanceScreenState.NoAccount -> BalanceNoAccount(backStack)
        is BalanceScreenState.HasAccount -> {
            BalanceForAccount(backStack, tmpAccount.accountViewItem)
        }

        else -> {}
    }
}
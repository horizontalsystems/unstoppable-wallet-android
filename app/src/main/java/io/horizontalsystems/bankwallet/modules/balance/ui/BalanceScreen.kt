package io.horizontalsystems.bankwallet.modules.balance.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import io.horizontalsystems.bankwallet.modules.balance.BalanceAccountsViewModel
import io.horizontalsystems.bankwallet.modules.balance.BalanceScreenState
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation

@Composable
fun BalanceScreen(
    navController: HSNavigation,
) {
    val viewModel = hiltViewModel<BalanceAccountsViewModel>()

    when (val tmpAccount = viewModel.balanceScreenState) {
        BalanceScreenState.NoAccount -> BalanceNoAccount(navController)
        is BalanceScreenState.HasAccount -> {
            BalanceForAccount(navController, tmpAccount.accountViewItem)
        }

        else -> {}
    }
}
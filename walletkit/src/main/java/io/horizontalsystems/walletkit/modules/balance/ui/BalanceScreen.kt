package io.horizontalsystems.walletkit.modules.balance.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.modules.balance.BalanceAccountsViewModel
import io.horizontalsystems.walletkit.modules.balance.BalanceModule
import io.horizontalsystems.walletkit.modules.balance.BalanceScreenState
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation

@Composable
fun BalanceScreen(
    navigation: HSNavigation,
) {
    val viewModel = viewModel<BalanceAccountsViewModel>(factory = BalanceModule.AccountsFactory())

    when (val tmpAccount = viewModel.balanceScreenState) {
        BalanceScreenState.NoAccount -> BalanceNoAccount(navigation)
        is BalanceScreenState.HasAccount -> {
            BalanceForAccount(navigation, tmpAccount.accountViewItem)
        }

        else -> {}
    }
}
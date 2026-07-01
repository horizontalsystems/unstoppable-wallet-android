package io.horizontalsystems.bankwallet.modules.balance.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.modules.balance.BalanceAccountsViewModel
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule
import io.horizontalsystems.bankwallet.modules.balance.BalanceScreenState
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation

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
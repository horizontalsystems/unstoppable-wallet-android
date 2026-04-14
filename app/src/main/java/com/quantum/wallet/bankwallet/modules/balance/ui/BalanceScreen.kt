package com.quantum.wallet.bankwallet.modules.balance.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.modules.balance.BalanceAccountsViewModel
import com.quantum.wallet.bankwallet.modules.balance.BalanceModule
import com.quantum.wallet.bankwallet.modules.balance.BalanceScreenState

@Composable
fun BalanceScreen(
    navController: NavController,
) {
    val viewModel = viewModel<BalanceAccountsViewModel>(factory = BalanceModule.AccountsFactory())

    when (val tmpAccount = viewModel.balanceScreenState) {
        BalanceScreenState.NoAccount -> BalanceNoAccount(navController)
        is BalanceScreenState.HasAccount -> {
            BalanceForAccount(navController, tmpAccount.accountViewItem)
        }

        else -> {}
    }
}
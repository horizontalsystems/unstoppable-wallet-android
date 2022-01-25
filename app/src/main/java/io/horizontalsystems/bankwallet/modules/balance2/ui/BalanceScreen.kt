package io.horizontalsystems.bankwallet.modules.balance2.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.modules.balance2.BalanceAccountsViewModel
import io.horizontalsystems.bankwallet.modules.balance2.BalanceModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun BalanceScreen(navController: NavController) {
    ComposeAppTheme {
        val viewModel = viewModel<BalanceAccountsViewModel>(factory = BalanceModule.AccountsFactory())

        when (val tmpAccount = viewModel.accountViewItem) {
            null -> BalanceNoAccount(navController)
            else -> BalanceForAccount(navController, tmpAccount)
        }
    }
}
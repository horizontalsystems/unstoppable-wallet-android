package io.horizontalsystems.bankwallet.modules.balance2.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.modules.balance.AccountViewItem

@Composable
fun BalanceItemsEmpty(navController: NavController, accountViewItem: AccountViewItem) {
    if (accountViewItem.isWatchAccount) {
        BalanceItemsEmptyWatchAccount()
    } else {
        BalanceItemsEmptyRegularAccount(navController)
    }
}

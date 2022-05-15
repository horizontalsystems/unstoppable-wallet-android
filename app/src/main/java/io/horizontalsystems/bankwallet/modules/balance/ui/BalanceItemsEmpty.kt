package io.horizontalsystems.bankwallet.modules.balance.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.balance.AccountViewItem
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.ScreenMessageWithAction

@Composable
fun BalanceItemsEmpty(navController: NavController, accountViewItem: AccountViewItem) {
    if (accountViewItem.isWatchAccount) {
        ListEmptyView(
            text = stringResource(R.string.Balance_WatchAccount_NoBalance),
            icon = R.drawable.ic_empty_wallet
        )
    } else {
        ScreenMessageWithAction(
            text = stringResource(R.string.Balance_NoCoinsAlert),
            icon = R.drawable.ic_add_to_wallet_2_48,
            action = Pair(stringResource(id = R.string.Balance_AddCoins)) {
                navController.slideFromRight(R.id.manageWalletsFragment)
            }
        )
    }
}

package io.horizontalsystems.bankwallet.modules.balance2.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.balance2.AccountViewItem
import io.horizontalsystems.bankwallet.modules.balance2.BalanceModule2
import io.horizontalsystems.bankwallet.modules.balance2.BalanceViewModel
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun BalanceForAccount(navController: NavController, accountViewItem: AccountViewItem) {
    val viewModel = viewModel<BalanceViewModel>(factory = BalanceModule2.BalanceXxxFactory())

    Column {
        TopAppBar(
            modifier = Modifier.height(56.dp),
            title = {
                Row(
                    modifier = Modifier
                        .clickable {
                            navController.slideFromBottom(
                                R.id.mainFragment_to_manageKeysFragment,
                                ManageAccountsModule.prepareParams(ManageAccountsModule.Mode.Switcher)
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = accountViewItem.name,
                        style = ComposeAppTheme.typography.title3,
                        color = ComposeAppTheme.colors.oz,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_down_24),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.grey
                    )
                }
            },
            backgroundColor = ComposeAppTheme.colors.tyler,
            elevation = 0.dp
        )

        viewModel.balanceViewItemsWrapper?.let { (headerViewItem, balanceViewItems) ->
            if (balanceViewItems.isNotEmpty()) {
                BalanceItems(
                    headerViewItem,
                    balanceViewItems,
                    viewModel,
                    accountViewItem,
                    navController
                )
            } else {
                BalanceItemsEmpty(navController, accountViewItem)
            }
        }
    }
}
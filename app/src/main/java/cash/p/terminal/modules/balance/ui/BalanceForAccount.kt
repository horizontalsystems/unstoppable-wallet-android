package cash.p.terminal.modules.balance.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.backupalert.BackupAlert
import cash.p.terminal.modules.balance.AccountViewItem
import cash.p.terminal.modules.balance.BalanceModule
import cash.p.terminal.modules.balance.BalanceViewModel
import cash.p.terminal.modules.manageaccounts.ManageAccountsModule
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.title3_leah

@Composable
fun BalanceForAccount(navController: NavController, accountViewItem: AccountViewItem) {
    val viewModel = viewModel<BalanceViewModel>(factory = BalanceModule.Factory())

    BackupAlert(navController)

    Column {
        AppBar(
            title = {
                BalanceTitleRow(navController, accountViewItem.name)
            },
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Nfts_Title),
                    icon = R.drawable.ic_nft_24,
                    onClick = {
                        navController.slideFromRight(R.id.nftsFragment)
                    }
                )
            )
        )

        val uiState = viewModel.uiState

        Crossfade(uiState.viewState) { viewState ->
            when (viewState) {
                ViewState.Success -> {
                    val balanceViewItems = uiState.balanceViewItems

                    if (balanceViewItems.isNotEmpty()) {
                        BalanceItems(
                            balanceViewItems,
                            viewModel,
                            accountViewItem,
                            navController,
                            uiState,
                            viewModel.totalUiState
                        )
                    } else {
                        BalanceItemsEmpty(navController, accountViewItem)
                    }
                }
                ViewState.Loading,
                is ViewState.Error -> {}
            }
        }
    }
}

@Composable
fun BalanceTitleRow(
    navController: NavController,
    title: String
) {
    Row(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                navController.slideFromBottom(
                    R.id.manageAccountsFragment,
                    ManageAccountsModule.prepareParams(ManageAccountsModule.Mode.Switcher)
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        title3_leah(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(weight = 1f, fill = false)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_down_24),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey
        )
    }
}
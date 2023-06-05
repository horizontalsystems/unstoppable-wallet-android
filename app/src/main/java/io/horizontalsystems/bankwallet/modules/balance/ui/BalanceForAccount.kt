package io.horizontalsystems.bankwallet.modules.balance.ui

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
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.backupalert.BackupAlert
import io.horizontalsystems.bankwallet.modules.balance.AccountViewItem
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewModel
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.title3_leah

@Composable
fun BalanceForAccount(navController: NavController, accountViewItem: AccountViewItem) {
    val viewModel = viewModel<BalanceViewModel>(factory = BalanceModule.Factory())

    BackupAlert(navController)

    Column {
        AppBar(
            title = {
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
                        text = accountViewItem.name,
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
package io.horizontalsystems.bankwallet.modules.balance.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.balance.AccountViewItem
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewModel
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun BalanceForAccount(navController: NavController, accountViewItem: AccountViewItem) {
    val viewModel = viewModel<BalanceViewModel>(factory = BalanceModule.Factory())

    Column {
        TopAppBar(
            modifier = Modifier.height(56.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
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
                            }
                            .weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = accountViewItem.name,
                            style = ComposeAppTheme.typography.title3,
                            color = ComposeAppTheme.colors.oz,
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
                    Icon(
                        modifier = Modifier
                            .clickable(
                                role = Role.Button,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(bounded = false, radius = 24.dp),
                                onClick = {
                                    navController.slideFromRight(R.id.nftsFragment)
                                }
                            )
                            .padding(16.dp),
                        painter = painterResource(id = R.drawable.ic_nft_24),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.jacob
                    )
                }
            },
            backgroundColor = ComposeAppTheme.colors.tyler,
            elevation = 0.dp
        )

        val uiState = viewModel.uiState

        Crossfade(uiState.viewState) { viewState ->
            when (viewState) {
                is ViewState.Success -> {
                    val balanceViewItems = uiState.balanceViewItems

                    if (balanceViewItems.isNotEmpty()) {
                        BalanceItems(
                            balanceViewItems,
                            viewModel,
                            accountViewItem,
                            navController,
                            uiState
                        )
                    } else {
                        BalanceItemsEmpty(navController, accountViewItem)
                    }
                }
            }
        }
    }
}
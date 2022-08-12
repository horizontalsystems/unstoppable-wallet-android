package io.horizontalsystems.bankwallet.modules.balance.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.balance.*
import io.horizontalsystems.bankwallet.modules.rateapp.RateAppModule
import io.horizontalsystems.bankwallet.modules.rateapp.RateAppViewModel
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.helpers.HudHelper

@Composable
fun BalanceItems(
    balanceViewItems: List<BalanceViewItem>,
    viewModel: BalanceViewModel,
    accountViewItem: AccountViewItem,
    navController: NavController,
    uiState: BalanceUiState
) {
    val rateAppViewModel = viewModel<RateAppViewModel>(factory = RateAppModule.Factory())
    DisposableEffect(true) {
        rateAppViewModel.onBalancePageActive()
        onDispose {
            rateAppViewModel.onBalancePageInactive()
        }
    }

    Column {
        val context = LocalContext.current

        when (val totalState = uiState.totalState) {
            TotalUIState.Hidden -> {
                DoubleText(
                    title = "*****",
                    body = "*****",
                    dimmed = false,
                    onClickTitle = {
                        viewModel.onBalanceClick()
                        HudHelper.vibrate(context)
                    },
                    onClickBody = {

                    }
                )
            }
            is TotalUIState.Visible -> {
                DoubleText(
                    title = totalState.currencyValueStr,
                    body = totalState.coinValueStr,
                    dimmed = totalState.dimmed,
                    onClickTitle = {
                        viewModel.onBalanceClick()
                        HudHelper.vibrate(context)
                    },
                    onClickBody = {
                        viewModel.toggleTotalType()
                        HudHelper.vibrate(context)
                    }
                )
            }
        }

        HeaderSorting(borderTop = true) {
            var showSortTypeSelectorDialog by remember { mutableStateOf(false) }

            ButtonSecondaryTransparent(
                title = stringResource(viewModel.sortType.getTitleRes()),
                iconRight = R.drawable.ic_down_arrow_20,
                onClick = {
                    showSortTypeSelectorDialog = true
                }
            )

            if (showSortTypeSelectorDialog) {
                SelectorDialogCompose(
                    title = stringResource(R.string.Balance_Sort_PopupTitle),
                    items = viewModel.sortTypes.map {
                        TabItem(stringResource(it.getTitleRes()), it == viewModel.sortType, it)
                    },
                    onDismissRequest = {
                        showSortTypeSelectorDialog = false
                    },
                    onSelectItem = {
                        viewModel.sortType = it
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (accountViewItem.isWatchAccount) {
                Image(
                    painter = painterResource(R.drawable.icon_binocule_24),
                    contentDescription = "binoculars icon"
                )
            }

            if (accountViewItem.manageCoinsAllowed) {
                ButtonSecondaryCircle(
                    icon = R.drawable.ic_manage_2,
                    onClick = {
                        navController.slideFromRight(R.id.manageWalletsFragment)
                    }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))
        }

        Wallets(balanceViewItems, viewModel, navController, accountViewItem.id, viewModel.sortType, uiState)
    }
}


@Composable
fun Wallets(
    balanceViewItems: List<BalanceViewItem>,
    viewModel: BalanceViewModel,
    navController: NavController,
    accountId: String,
    sortType: BalanceSortType,
    uiState: BalanceUiState
) {
    var revealedCardId by remember { mutableStateOf<Int?>(null) }

    val listState = rememberSaveable(
        accountId,
        sortType,
        saver = LazyListState.Saver
    ) {
        LazyListState()
    }

    HSSwipeRefresh(
        state = rememberSwipeRefreshState(uiState.isRefreshing),
        onRefresh = {
            viewModel.onRefresh()
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = 8.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(balanceViewItems, key = { item -> item.wallet.hashCode() }) { item ->
                if (item.isWatchAccount) {
                    BalanceCard(item, viewModel, navController)
                } else {
                    BalanceCardSwipable(
                        viewItem = item,
                        viewModel = viewModel,
                        navController = navController,
                        revealed = revealedCardId == item.wallet.hashCode(),
                        onReveal = { walletHashCode ->
                            if (revealedCardId != walletHashCode) {
                                revealedCardId = walletHashCode
                            }
                        },
                        onConceal = {
                            revealedCardId = null
                        },
                    )
                }
            }
        }
    }
}



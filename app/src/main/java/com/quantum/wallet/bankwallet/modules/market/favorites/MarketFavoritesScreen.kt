package com.quantum.wallet.bankwallet.modules.market.favorites

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.paidAction
import com.quantum.wallet.bankwallet.core.slideFromBottomForResult
import com.quantum.wallet.bankwallet.core.slideFromRight
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.StatPremiumTrigger
import com.quantum.wallet.bankwallet.core.stats.StatSection
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.core.stats.statPeriod
import com.quantum.wallet.bankwallet.core.stats.statSortType
import com.quantum.wallet.bankwallet.entities.ViewState
import com.quantum.wallet.bankwallet.modules.coin.CoinFragment
import com.quantum.wallet.bankwallet.modules.coin.overview.ui.Loading
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.HSSwipeRefresh
import com.quantum.wallet.bankwallet.ui.compose.components.CoinListOrderable
import com.quantum.wallet.bankwallet.uiv3.components.menu.MenuGroup
import com.quantum.wallet.bankwallet.uiv3.components.menu.MenuItemX
import com.quantum.wallet.bankwallet.ui.compose.components.HSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.HeaderSorting
import com.quantum.wallet.bankwallet.ui.compose.components.ListEmptyView
import com.quantum.wallet.bankwallet.ui.compose.components.ListErrorView
import com.quantum.wallet.bankwallet.uiv3.components.controls.ButtonSize
import com.quantum.wallet.bankwallet.uiv3.components.controls.ButtonStyle
import com.quantum.wallet.bankwallet.uiv3.components.controls.ButtonVariant
import com.quantum.wallet.bankwallet.uiv3.components.controls.HSButton
import com.quantum.wallet.bankwallet.uiv3.components.controls.HSDropdownButton
import com.quantum.wallet.bankwallet.uiv3.components.controls.HSIconButton
import com.quantum.wallet.subscriptions.core.TradeSignals

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarketFavoritesScreen(
    navController: NavController
) {
    val viewModel = viewModel<MarketFavoritesViewModel>(factory = MarketFavoritesModule.Factory())
    val uiState = viewModel.uiState
    var openSortingSelector by rememberSaveable { mutableStateOf(false) }
    var openPeriodSelector by rememberSaveable { mutableStateOf(false) }
    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }
    var manualOrderEnabled by rememberSaveable { mutableStateOf(false) }

    HSSwipeRefresh(
        refreshing = uiState.isRefreshing,
        topPadding = 44,
        onRefresh = {
            viewModel.refresh()

            stat(
                page = StatPage.Markets,
                event = StatEvent.Refresh,
                section = StatSection.Watchlist
            )
        }
    ) {
        Crossfade(
            targetState = uiState.viewState,
            label = ""
        ) { viewState ->
            when (viewState) {
                ViewState.Loading -> {
                    Loading()
                }

                is ViewState.Error -> {
                    ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                }

                ViewState.Success -> {
                    if (uiState.viewItems.isEmpty()) {
                        ListEmptyView(
                            text = stringResource(R.string.Market_Tab_Watchlist_EmptyList),
                            icon = R.drawable.ic_heart_24
                        )
                    } else {
                        CoinListOrderable(
                            items = uiState.viewItems,
                            scrollToTop = scrollToTopAfterUpdate,
                            onAddFavorite = { /*not used */ },
                            onRemoveFavorite = { uid ->
                                viewModel.removeFromFavorites(uid)

                                stat(
                                    page = StatPage.Markets,
                                    event = StatEvent.RemoveFromWatchlist(uid),
                                    section = StatSection.Watchlist
                                )
                            },
                            onCoinClick = { coinUid ->
                                val arguments = CoinFragment.Input(coinUid)
                                navController.slideFromRight(R.id.coinFragment, arguments)

                                stat(
                                    page = StatPage.Markets,
                                    event = StatEvent.OpenCoin(coinUid),
                                    section = StatSection.Watchlist
                                )
                            },
                            onReorder = { from, to ->
                                viewModel.reorder(from, to)
                            },
                            canReorder = uiState.sortingField == WatchlistSorting.Manual,
                            showReorderArrows = uiState.sortingField == WatchlistSorting.Manual && manualOrderEnabled,
                            enableManualOrder = {
                                manualOrderEnabled = true
                            },
                            preItems = {
                                stickyHeader {
                                    HeaderSorting(
                                        borderBottom = true,
                                        backgroundColor = ComposeAppTheme.colors.lawrence
                                    ) {
                                        HSpacer(width = 16.dp)
                                        HSDropdownButton(
                                            variant = ButtonVariant.Secondary,
                                            title = stringResource(uiState.sortingField.titleResId),
                                            onClick = {
                                                openSortingSelector = true
                                            }
                                        )
                                        if (uiState.sortingField == WatchlistSorting.Manual) {
                                            HSpacer(width = 12.dp)
                                            HSIconButton(
                                                variant = ButtonVariant.Secondary,
                                                size = ButtonSize.Small,
                                                icon = painterResource(R.drawable.ic_edit_20),
                                                onClick = {
                                                    manualOrderEnabled = !manualOrderEnabled
                                                }
                                            )
                                        }
                                        HSpacer(width = 12.dp)
                                        HSDropdownButton(
                                            variant = ButtonVariant.Secondary,
                                            title = stringResource(uiState.period.titleResId),
                                            onClick = {
                                                openPeriodSelector = true
                                            }
                                        )
                                        HSpacer(width = 12.dp)
                                        HSButton(
                                            variant = ButtonVariant.Secondary,
                                            style = ButtonStyle.Solid,
                                            size = ButtonSize.Small,
                                            title = stringResource(id = R.string.Market_Signals),
                                            onClick = {
                                                if (!uiState.showSignal) {
                                                    navController.paidAction(TradeSignals) {
                                                        navController.slideFromBottomForResult<MarketSignalsFragment.Result>(
                                                            R.id.marketSignalsFragment
                                                        ) {
                                                            if (it.enabled) {
                                                                viewModel.showSignals()
                                                            }
                                                        }
                                                    }
                                                    stat(
                                                        page = StatPage.MarketOverview,
                                                        event = StatEvent.OpenPremium(
                                                            StatPremiumTrigger.TradingSignal),
                                                        section = StatSection.Watchlist
                                                    )
                                                } else {
                                                    viewModel.hideSignals()
                                                }
                                            }
                                        )
                                        HSpacer(width = 16.dp)
                                    }
                                }
                            }
                        )
                        if (scrollToTopAfterUpdate) {
                            scrollToTopAfterUpdate = false
                        }
                    }
                }
            }
        }
    }

    if (openSortingSelector) {
        MenuGroup(
            title = stringResource(R.string.Market_Sort_PopupTitle),
            items = viewModel.sortingOptions.map {
                MenuItemX(stringResource(it.titleResId), it == uiState.sortingField, it)
            },
            onDismissRequest = { openSortingSelector = false },
            onSelectItem = { selected ->
                manualOrderEnabled = false
                scrollToTopAfterUpdate = true
                viewModel.onSelectSortingField(selected)
                stat(
                    page = StatPage.Markets,
                    event = StatEvent.SwitchSortType(selected.statSortType),
                    section = StatSection.Watchlist
                )
            }
        )
    }
    if (openPeriodSelector) {
        MenuGroup(
            title = stringResource(R.string.CoinPage_Period),
            items = viewModel.periods.map {
                MenuItemX(stringResource(it.titleResId), it == uiState.period, it)
            },
            onDismissRequest = { openPeriodSelector = false },
            onSelectItem = { selected ->
                scrollToTopAfterUpdate = true
                viewModel.onSelectPeriod(selected)
                stat(
                    page = StatPage.Markets,
                    event = StatEvent.SwitchPeriod(selected.statPeriod),
                    section = StatSection.Watchlist
                )
            }
        )
    }

}

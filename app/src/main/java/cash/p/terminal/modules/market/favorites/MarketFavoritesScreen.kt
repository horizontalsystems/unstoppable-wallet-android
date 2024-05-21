package cash.p.terminal.modules.market.favorites

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.stat
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.CoinFragment
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.modules.market.topcoins.OptionController
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.HSSwipeRefresh
import cash.p.terminal.ui.compose.Select
import cash.p.terminal.ui.compose.components.AlertGroup
import cash.p.terminal.ui.compose.components.ButtonSecondaryCircle
import cash.p.terminal.ui.compose.components.ButtonSecondaryDefault
import cash.p.terminal.ui.compose.components.ButtonSecondaryYellow
import cash.p.terminal.ui.compose.components.CoinListOrderable
import cash.p.terminal.ui.compose.components.HSpacer
import cash.p.terminal.ui.compose.components.HeaderSorting
import cash.p.terminal.ui.compose.components.ListEmptyView
import cash.p.terminal.ui.compose.components.ListErrorView

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarketFavoritesScreen(
    navController: NavController,
    viewModel: MarketFavoritesViewModel = viewModel(factory = MarketFavoritesModule.Factory())
) {
    val uiState = viewModel.uiState
    var openSortingSelector by rememberSaveable { mutableStateOf(false) }
    var openPeriodSelector by rememberSaveable { mutableStateOf(false) }
    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }
    var manualOrderEnabled by rememberSaveable { mutableStateOf(false) }

    HSSwipeRefresh(
        refreshing = uiState.isRefreshing,
        onRefresh = {
            viewModel.refresh()

            stat(page = StatPage.Watchlist, event = StatEvent.Refresh)
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
                            icon = R.drawable.ic_rate_24
                        )
                    } else {
                        CoinListOrderable(
                            items = uiState.viewItems,
                            scrollToTop = scrollToTopAfterUpdate,
                            onAddFavorite = { /*not used */ },
                            onRemoveFavorite = { uid ->
                                viewModel.removeFromFavorites(uid)

                                stat(
                                    page = StatPage.Watchlist,
                                    event = StatEvent.RemoveFromWatchlist(uid)
                                )
                            },
                            onCoinClick = { coinUid ->
                                val arguments = CoinFragment.Input(coinUid)
                                navController.slideFromRight(R.id.coinFragment, arguments)

                                stat(page = StatPage.Watchlist, event = StatEvent.OpenCoin(coinUid))
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
                                    ) {
                                        HSpacer(width = 16.dp)
                                        OptionController(
                                            uiState.sortingField.titleResId,
                                            onOptionClick = {
                                                openSortingSelector = true
                                            }
                                        )
                                        if (uiState.sortingField == WatchlistSorting.Manual) {
                                            HSpacer(width = 12.dp)
                                            ButtonSecondaryCircle(
                                                icon = R.drawable.ic_edit_20,
                                                tint = if (manualOrderEnabled) ComposeAppTheme.colors.dark else ComposeAppTheme.colors.leah,
                                                background = if (manualOrderEnabled) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.steel20,
                                            ) {
                                                manualOrderEnabled = !manualOrderEnabled
                                            }
                                        }
                                        HSpacer(width = 12.dp)
                                        OptionController(
                                            uiState.period.titleResId,
                                            onOptionClick = {
                                                openPeriodSelector = true
                                            }
                                        )
                                        HSpacer(width = 12.dp)
                                        SignalButton(
                                            turnedOn = uiState.showSignal,
                                            onClick = {
                                                viewModel.onToggleSignal()
                                            })
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

                null -> {}
            }
        }
    }

    if (openSortingSelector) {
        AlertGroup(
            title = R.string.Market_Sort_PopupTitle,
            select = Select(uiState.sortingField, viewModel.sortingOptions),
            onSelect = { selected ->
                manualOrderEnabled = false
                openSortingSelector = false
                viewModel.onSelectSortingField(selected)
            },
            onDismiss = {
                openSortingSelector = false
            }
        )
    }
    if (openPeriodSelector) {
        AlertGroup(
            title = R.string.Market_Tab_Coins,
            select = Select(uiState.period, viewModel.periods),
            onSelect = { selected ->
                viewModel.onSelectPeriod(selected)
                openPeriodSelector = false
            },
            onDismiss = {
                openPeriodSelector = false
            }
        )
    }

}

@Composable
fun SignalButton(turnedOn: Boolean, onClick: () -> Unit) {
    val title = stringResource(id = R.string.Market_Signals)
    if (turnedOn) {
        ButtonSecondaryYellow(
            title = title,
            onClick = onClick
        )
    } else {
        ButtonSecondaryDefault(
            title = title,
            onClick = onClick
        )
    }
}

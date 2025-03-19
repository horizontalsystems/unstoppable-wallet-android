package io.horizontalsystems.bankwallet.modules.market.favorites

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
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.paidAction
import io.horizontalsystems.bankwallet.core.slideFromBottomForResult
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statPeriod
import io.horizontalsystems.bankwallet.core.stats.statSortType
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.market.filtersresult.SignalButton
import io.horizontalsystems.bankwallet.modules.market.topcoins.OptionController
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.components.AlertGroup
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.CoinListOrderable
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderSorting
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.subscriptions.core.TradeSignals

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

            stat(page = StatPage.Markets,  section = StatSection.Watchlist, event = StatEvent.Refresh)
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

                                stat(page = StatPage.Markets,  section = StatSection.Watchlist, event = StatEvent.RemoveFromWatchlist(uid))
                            },
                            onCoinClick = { coinUid ->
                                val arguments = CoinFragment.Input(coinUid)
                                navController.slideFromRight(R.id.coinFragment, arguments)

                                stat(page = StatPage.Markets, section = StatSection.Watchlist, event = StatEvent.OpenCoin(coinUid))
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
                                            onToggle = {
                                                if (it) {
                                                    navController.paidAction(TradeSignals) {
                                                        navController.slideFromBottomForResult<MarketSignalsFragment.Result>(
                                                            R.id.marketSignalsFragment
                                                        ) {
                                                            if (it.enabled) {
                                                                viewModel.showSignals()
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    viewModel.hideSignals()
                                                }
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
                scrollToTopAfterUpdate = true
                viewModel.onSelectSortingField(selected)

                stat(page = StatPage.Markets, section = StatSection.Watchlist, event = StatEvent.SwitchSortType(selected.statSortType))
            },
            onDismiss = {
                openSortingSelector = false
            }
        )
    }
    if (openPeriodSelector) {
        AlertGroup(
            title = R.string.CoinPage_Period,
            select = Select(uiState.period, viewModel.periods),
            onSelect = { selected ->
                openPeriodSelector = false
                scrollToTopAfterUpdate = true
                viewModel.onSelectPeriod(selected)

                stat(page = StatPage.Markets, section = StatSection.Watchlist, event = StatEvent.SwitchPeriod(selected.statPeriod))
            },
            onDismiss = {
                openPeriodSelector = false
            }
        )
    }

}

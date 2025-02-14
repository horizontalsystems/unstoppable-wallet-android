package io.horizontalsystems.bankwallet.modules.market.topcoins

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statMarketTop
import io.horizontalsystems.bankwallet.core.stats.statPeriod
import io.horizontalsystems.bankwallet.core.stats.statSortType
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.components.AlertGroup
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryWithIcon
import io.horizontalsystems.bankwallet.ui.compose.components.CoinListSlidable
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderSorting
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopCoins(
    onCoinClick: (String) -> Unit,
) {
    val viewModel = viewModel<MarketTopCoinsViewModel>(
        factory = MarketTopCoinsViewModel.Factory(
            TopMarket.Top100,
            SortingField.TopGainers,
        )
    )

    var openSortingSelector by rememberSaveable { mutableStateOf(false) }
    var openTopSelector by rememberSaveable { mutableStateOf(false) }
    var openPeriodSelector by rememberSaveable { mutableStateOf(false) }

    val uiState = viewModel.uiState

    HSSwipeRefresh(
        refreshing = uiState.isRefreshing,
        topPadding = 44,
        onRefresh = {
            viewModel.refresh()

            stat(page = StatPage.Markets, section = StatSection.Coins, event = StatEvent.Refresh)
        }
    ) {
        Crossfade(uiState.viewState, label = "") { viewState ->
            when (viewState) {
                ViewState.Loading -> {
                    Loading()
                }

                is ViewState.Error -> {
                    ListErrorView(stringResource(R.string.SyncError), viewModel::refresh)
                }

                ViewState.Success -> {
                    val listState = rememberLazyListState()

                    LaunchedEffect(uiState.period, uiState.topMarket, uiState.sortingField) {
                        listState.scrollToItem(0)
                    }

                    CoinListSlidable(
                        listState = listState,
                        items = uiState.viewItems,
                        scrollToTop = false,
                        onAddFavorite = { uid ->
                            viewModel.onAddFavorite(uid)

                            stat(page = StatPage.Markets, section = StatSection.Coins, event = StatEvent.AddToWatchlist(uid))

                        },
                        onRemoveFavorite = { uid ->
                            viewModel.onRemoveFavorite(uid)

                            stat(page = StatPage.Markets, section = StatSection.Coins, event = StatEvent.RemoveFromWatchlist(uid))
                        },
                        onCoinClick = onCoinClick,
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
                                    HSpacer(width = 12.dp)
                                    OptionController(
                                        uiState.topMarket.titleResId,
                                        onOptionClick = {
                                            openTopSelector = true
                                        }
                                    )
                                    HSpacer(width = 12.dp)
                                    OptionController(
                                        uiState.period.titleResId,
                                        onOptionClick = {
                                            openPeriodSelector = true
                                        }
                                    )
                                    HSpacer(width = 16.dp)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (openSortingSelector) {
        AlertGroup(
            title = R.string.Market_Sort_PopupTitle,
            select = Select(uiState.sortingField, uiState.sortingFields),
            onSelect = { selected ->
                viewModel.onSelectSortingField(selected)
                openSortingSelector = false

                stat(page = StatPage.Markets, section = StatSection.Coins, event = StatEvent.SwitchSortType(selected.statSortType))
            },
            onDismiss = {
                openSortingSelector = false
            }
        )
    }
    if (openTopSelector) {
        AlertGroup(
            title = R.string.Market_Tab_Coins,
            select = Select(uiState.topMarket, uiState.topMarkets),
            onSelect = {
                viewModel.onSelectTopMarket(it)
                openTopSelector = false

                stat(page = StatPage.Markets, section = StatSection.Coins, event = StatEvent.SwitchMarketTop(it.statMarketTop))
            },
            onDismiss = {
                openTopSelector = false
            }
        )
    }
    if (openPeriodSelector) {
        AlertGroup(
            title = R.string.CoinPage_Period,
            select = Select(uiState.period, uiState.periods),
            onSelect = { selected ->
                viewModel.onSelectPeriod(selected)
                openPeriodSelector = false

                stat(page = StatPage.Markets, section = StatSection.Coins, event = StatEvent.SwitchPeriod(selected.statPeriod))
            },
            onDismiss = {
                openPeriodSelector = false
            }
        )
    }
}

@Composable
fun OptionController(
    label: Int,
    onOptionClick: () -> Unit
) {
    ButtonSecondaryWithIcon(
        modifier = Modifier.height(28.dp),
        onClick = onOptionClick,
        title = stringResource(label),
        iconRight = painterResource(R.drawable.ic_down_arrow_20),
    )
}
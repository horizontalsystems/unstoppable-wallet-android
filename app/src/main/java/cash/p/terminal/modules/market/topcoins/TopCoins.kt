package cash.p.terminal.modules.market.topcoins

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
import cash.p.terminal.R
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.StatSection
import io.horizontalsystems.core.entities.ViewState
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.TopMarket
import cash.p.terminal.ui_compose.components.HSSwipeRefresh
import cash.p.terminal.ui.compose.Select
import cash.p.terminal.ui.compose.components.AlertGroup
import cash.p.terminal.ui.compose.components.ButtonSecondaryWithIcon
import cash.p.terminal.ui.compose.components.CoinList
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HeaderSorting
import cash.p.terminal.ui.compose.components.ListErrorView

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
        onRefresh = {
            viewModel.refresh()
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

                    CoinList(
                        listState = listState,
                        items = uiState.viewItems,
                        scrollToTop = false,
                        onAddFavorite = { uid ->
                            viewModel.onAddFavorite(uid)

                        },
                        onRemoveFavorite = { uid ->
                            viewModel.onRemoveFavorite(uid)
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
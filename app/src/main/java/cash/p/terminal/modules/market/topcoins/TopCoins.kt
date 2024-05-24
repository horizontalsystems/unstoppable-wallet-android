package cash.p.terminal.modules.market.topcoins

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
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
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.modules.market.MarketField
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.TopMarket
import cash.p.terminal.ui.compose.HSSwipeRefresh
import cash.p.terminal.ui.compose.Select
import cash.p.terminal.ui.compose.components.AlertGroup
import cash.p.terminal.ui.compose.components.ButtonSecondaryWithIcon
import cash.p.terminal.ui.compose.components.CoinList
import cash.p.terminal.ui.compose.components.HSpacer
import cash.p.terminal.ui.compose.components.HeaderSorting
import cash.p.terminal.ui.compose.components.ListErrorView

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopCoins(
    onCoinClick: (String) -> Unit,
    viewModel: MarketTopCoinsViewModel = viewModel(
        factory = MarketTopCoinsModule.Factory(
            TopMarket.Top100,
            SortingField.TopGainers,
            MarketField.PriceDiff
        )
    )
) {

    var openSortingSelector by rememberSaveable { mutableStateOf(false) }
    var openTopSelector by rememberSaveable { mutableStateOf(false) }
    var openPeriodSelector by rememberSaveable { mutableStateOf(false) }
    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }

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
                    ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                }

                ViewState.Success -> {
                    CoinList(
                        items = uiState.viewItems,
                        scrollToTop = scrollToTopAfterUpdate,
                        onAddFavorite = { uid -> viewModel.onAddFavorite(uid) },
                        onRemoveFavorite = { uid -> viewModel.onRemoveFavorite(uid) },
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
                                        uiState.timeDuration.titleResId,
                                        onOptionClick = {
                                            openPeriodSelector = true
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

    if (openSortingSelector) {
        AlertGroup(
            title = R.string.Market_Sort_PopupTitle,
            select = Select(uiState.sortingField, viewModel.sortingFields),
            onSelect = { selected ->
                viewModel.onSelectSortingField(selected)
                openSortingSelector = false
                scrollToTopAfterUpdate = true
            },
            onDismiss = {
                openSortingSelector = false
            }
        )
    }
    if (openTopSelector) {
        AlertGroup(
            title = R.string.Market_Tab_Coins,
            select = Select(uiState.topMarket, viewModel.topMarkets),
            onSelect = {
                viewModel.onSelectTopMarket(it)
                openTopSelector = false
                scrollToTopAfterUpdate = true
            },
            onDismiss = {
                openTopSelector = false
            }
        )
    }
    if (openPeriodSelector) {
        AlertGroup(
            title = R.string.Market_Tab_Coins,
            select = Select(uiState.timeDuration, viewModel.periods),
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
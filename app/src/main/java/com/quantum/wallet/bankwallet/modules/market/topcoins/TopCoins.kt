package com.quantum.wallet.bankwallet.modules.market.topcoins

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
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.StatSection
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.core.stats.statMarketTop
import com.quantum.wallet.bankwallet.core.stats.statPeriod
import com.quantum.wallet.bankwallet.core.stats.statSortType
import com.quantum.wallet.bankwallet.entities.ViewState
import com.quantum.wallet.bankwallet.modules.coin.overview.ui.Loading
import com.quantum.wallet.bankwallet.modules.market.SortingField
import com.quantum.wallet.bankwallet.modules.market.TopMarket
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.HSSwipeRefresh
import com.quantum.wallet.bankwallet.ui.compose.components.ButtonSecondaryWithIcon
import com.quantum.wallet.bankwallet.uiv3.components.menu.MenuGroup
import com.quantum.wallet.bankwallet.uiv3.components.menu.MenuItemX
import com.quantum.wallet.bankwallet.ui.compose.components.CoinListSlidable
import com.quantum.wallet.bankwallet.ui.compose.components.HSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.HeaderSorting
import com.quantum.wallet.bankwallet.ui.compose.components.ListErrorView
import com.quantum.wallet.bankwallet.uiv3.components.controls.ButtonVariant
import com.quantum.wallet.bankwallet.uiv3.components.controls.HSDropdownButton

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

            stat(page = StatPage.Markets, event = StatEvent.Refresh, section = StatSection.Coins)
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

                            stat(
                                page = StatPage.Markets,
                                event = StatEvent.AddToWatchlist(uid),
                                section = StatSection.Coins
                            )

                        },
                        onRemoveFavorite = { uid ->
                            viewModel.onRemoveFavorite(uid)

                            stat(
                                page = StatPage.Markets,
                                event = StatEvent.RemoveFromWatchlist(uid),
                                section = StatSection.Coins
                            )
                        },
                        onCoinClick = onCoinClick,
                        bottomPadding = 140.dp,
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
                                    HSpacer(width = 12.dp)
                                    HSDropdownButton(
                                        variant = ButtonVariant.Secondary,
                                        title = stringResource(uiState.topMarket.titleResId),
                                        onClick = {
                                            openTopSelector = true
                                        }
                                    )
                                    HSpacer(width = 12.dp)
                                    HSDropdownButton(
                                        variant = ButtonVariant.Secondary,
                                        title = stringResource(uiState.period.titleResId),
                                        onClick = {
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
        MenuGroup(
            title = stringResource(R.string.Market_Sort_PopupTitle),
            items = uiState.sortingFields.map {
                MenuItemX(stringResource(it.titleResId), it == uiState.sortingField, it)
            },
            onDismissRequest = { openSortingSelector = false },
            onSelectItem = { selected ->
                viewModel.onSelectSortingField(selected)
                stat(
                    page = StatPage.Markets,
                    event = StatEvent.SwitchSortType(selected.statSortType),
                    section = StatSection.Coins
                )
            }
        )
    }
    if (openTopSelector) {
        MenuGroup(
            title = stringResource(R.string.Market_Tab_Coins),
            items = uiState.topMarkets.map {
                MenuItemX(stringResource(it.titleResId), it == uiState.topMarket, it)
            },
            onDismissRequest = { openTopSelector = false },
            onSelectItem = {
                viewModel.onSelectTopMarket(it)
                stat(
                    page = StatPage.Markets,
                    event = StatEvent.SwitchMarketTop(it.statMarketTop),
                    section = StatSection.Coins
                )
            }
        )
    }
    if (openPeriodSelector) {
        MenuGroup(
            title = stringResource(R.string.CoinPage_Period),
            items = uiState.periods.map {
                MenuItemX(stringResource(it.titleResId), it == uiState.period, it)
            },
            onDismissRequest = { openPeriodSelector = false },
            onSelectItem = { selected ->
                viewModel.onSelectPeriod(selected)
                stat(
                    page = StatPage.Markets,
                    event = StatEvent.SwitchPeriod(selected.statPeriod),
                    section = StatSection.Coins
                )
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
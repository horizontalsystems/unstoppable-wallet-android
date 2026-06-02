package io.horizontalsystems.bankwallet.modules.market.filtersresult

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatPremiumTrigger
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.CoinPage
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.market.favorites.MarketSignalsPage
import io.horizontalsystems.bankwallet.modules.market.filters.MarketFiltersPage
import io.horizontalsystems.bankwallet.modules.market.filters.MarketFiltersViewModel
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.ui.compose.components.CoinList
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderSorting
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonStyle
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSDropdownButton
import io.horizontalsystems.bankwallet.uiv3.components.menu.MenuGroup
import io.horizontalsystems.bankwallet.uiv3.components.menu.MenuItemX
import io.horizontalsystems.subscriptions.core.TradeSignals
import kotlinx.serialization.Serializable
import io.horizontalsystems.bankwallet.modules.nav3.viewModelForScreen

@Serializable
data object MarketFiltersResultsPage : HSPage() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        val marketSearchFilterViewModel = navController.viewModelForScreen<MarketFiltersViewModel>(MarketFiltersPage::class)
        val viewModel = hiltViewModel<MarketFiltersResultViewModel, MarketFiltersResultViewModel.Factory> { factory ->
            factory.create(marketSearchFilterViewModel.service)
        }

        SearchResultsScreen(viewModel, navController)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchResultsScreen(
    viewModel: MarketFiltersResultViewModel,
    navController: HSNavigation
) {

    val uiState = viewModel.uiState
    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }
    var openSortingSelector by rememberSaveable { mutableStateOf(false) }

    HSScaffold(
        title = stringResource(R.string.Market_AdvancedSearch_Results),
        onBack = navController::removeLastOrNull,
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Crossfade(uiState.viewState, label = "") { state ->
                when (state) {
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
                            onAddFavorite = { uid ->
                                viewModel.onAddFavorite(uid)

                                stat(
                                    page = StatPage.AdvancedSearchResults,
                                    event = StatEvent.AddToWatchlist(uid)
                                )
                            },
                            onRemoveFavorite = { uid ->
                                viewModel.onRemoveFavorite(uid)

                                stat(
                                    page = StatPage.AdvancedSearchResults,
                                    event = StatEvent.RemoveFromWatchlist(uid)
                                )
                            },
                            onCoinClick = { coinUid ->
                                val arguments = CoinPage.Input(coinUid)
                                navController.slideFromRight(CoinPage(arguments))

                                stat(
                                    page = StatPage.AdvancedSearchResults,
                                    event = StatEvent.OpenCoin(coinUid)
                                )
                            },
                            preItems = {
                                stickyHeader {
                                    HeaderSorting(borderBottom = true, borderTop = true) {
                                        HSpacer(width = 16.dp)
                                        HSDropdownButton(
                                            variant = ButtonVariant.Secondary,
                                            title = stringResource(uiState.sortingField.titleResId),
                                            onClick = {
                                                openSortingSelector = true
                                            }
                                        )
                                        HSpacer(width = 12.dp)
                                        val forResult = navController.slideFromBottomForResult<MarketSignalsPage.Result>(
                                            { MarketSignalsPage }
                                        ) {
                                            if (it.enabled) {
                                                viewModel.showSignals()
                                            }
                                        }
                                        HSButton(
                                            variant = ButtonVariant.Secondary,
                                            style = ButtonStyle.Solid,
                                            size = ButtonSize.Small,
                                            title = stringResource(id = R.string.Market_Signals),
                                            onClick = {
                                                if (!uiState.showSignal) {
                                                    navController.paidAction(TradeSignals, forResult)
                                                    stat(
                                                        page = StatPage.AdvancedSearchResults,
                                                        event = StatEvent.OpenPremium(
                                                            StatPremiumTrigger.TradingSignal
                                                        )
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

            if (openSortingSelector) {
                MenuGroup(
                    title = stringResource(R.string.Market_Sort_PopupTitle),
                    items = uiState.selectSortingField.options.map {
                        MenuItemX(stringResource(it.titleResId), it == uiState.selectSortingField.selected, it)
                    },
                    onDismissRequest = { openSortingSelector = false },
                    onSelectItem = { selected ->
                        viewModel.onSelectSortingField(selected)
                        scrollToTopAfterUpdate = true
                    }
                )
            }
        }
    }
}
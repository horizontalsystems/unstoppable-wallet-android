package com.quantum.wallet.bankwallet.modules.market.filtersresult

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
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.paidAction
import com.quantum.wallet.bankwallet.core.slideFromBottomForResult
import com.quantum.wallet.bankwallet.core.slideFromRight
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.StatPremiumTrigger
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.entities.ViewState
import com.quantum.wallet.bankwallet.modules.coin.CoinFragment
import com.quantum.wallet.bankwallet.modules.coin.overview.ui.Loading
import com.quantum.wallet.bankwallet.modules.market.favorites.MarketSignalsFragment
import com.quantum.wallet.bankwallet.modules.market.filters.MarketFiltersViewModel
import com.quantum.wallet.bankwallet.ui.compose.components.CoinList
import com.quantum.wallet.bankwallet.uiv3.components.menu.MenuGroup
import com.quantum.wallet.bankwallet.uiv3.components.menu.MenuItemX
import com.quantum.wallet.bankwallet.ui.compose.components.HSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.HeaderSorting
import com.quantum.wallet.bankwallet.ui.compose.components.ListErrorView
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import com.quantum.wallet.bankwallet.uiv3.components.controls.ButtonSize
import com.quantum.wallet.bankwallet.uiv3.components.controls.ButtonStyle
import com.quantum.wallet.bankwallet.uiv3.components.controls.ButtonVariant
import com.quantum.wallet.bankwallet.uiv3.components.controls.HSButton
import com.quantum.wallet.bankwallet.uiv3.components.controls.HSDropdownButton
import com.quantum.wallet.subscriptions.core.TradeSignals

class MarketFiltersResultsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val viewModel = getViewModel()

        if (viewModel == null) {
            navController.popBackStack()
            return
        }

        SearchResultsScreen(viewModel, navController)
    }

    private fun getViewModel(): MarketFiltersResultViewModel? {
        return try {
            val marketSearchFilterViewModel by navGraphViewModels<MarketFiltersViewModel>(R.id.marketAdvancedSearchFragment)
            val viewModel by viewModels<MarketFiltersResultViewModel> {
                MarketFiltersResultsModule.Factory(marketSearchFilterViewModel.service)
            }
            viewModel
        } catch (e: RuntimeException) {
            null
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchResultsScreen(
    viewModel: MarketFiltersResultViewModel,
    navController: NavController
) {

    val uiState = viewModel.uiState
    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }
    var openSortingSelector by rememberSaveable { mutableStateOf(false) }

    HSScaffold(
        title = stringResource(R.string.Market_AdvancedSearch_Results),
        onBack = navController::popBackStack,
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
                                val arguments = CoinFragment.Input(coinUid)
                                navController.slideFromRight(R.id.coinFragment, arguments)

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
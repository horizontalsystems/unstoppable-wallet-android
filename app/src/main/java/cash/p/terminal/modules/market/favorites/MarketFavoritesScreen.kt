package cash.p.terminal.modules.market.favorites

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.CoinFragment
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.modules.market.favorites.MarketFavoritesModule.Period
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.HSSwipeRefresh
import cash.p.terminal.ui.compose.Select
import cash.p.terminal.ui.compose.components.ButtonSecondaryCircle
import cash.p.terminal.ui.compose.components.ButtonSecondaryToggle
import cash.p.terminal.ui.compose.components.CoinList
import cash.p.terminal.ui.compose.components.HeaderSorting
import cash.p.terminal.ui.compose.components.ListEmptyView
import cash.p.terminal.ui.compose.components.ListErrorView

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarketFavoritesScreen(
    navController: NavController,
    viewModel: MarketFavoritesViewModel = viewModel(factory = MarketFavoritesModule.Factory())
) {
    val viewState by viewModel.viewStateLiveData.observeAsState()
    val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
    val marketFavoritesData by viewModel.viewItemLiveData.observeAsState()
    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }

    HSSwipeRefresh(
        refreshing = isRefreshing,
        onRefresh = {
            viewModel.refresh()
        }
    ) {
        Crossfade(
            targetState = viewState,
            modifier = Modifier.background(color = ComposeAppTheme.colors.tyler),
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
                    marketFavoritesData?.let { data ->
                        if (data.marketItems.isEmpty()) {
                            ListEmptyView(
                                text = stringResource(R.string.Market_Tab_Watchlist_EmptyList),
                                icon = R.drawable.ic_rate_24
                            )
                        } else {
                            CoinList(
                                items = data.marketItems,
                                scrollToTop = scrollToTopAfterUpdate,
                                onAddFavorite = { /*not used */ },
                                onRemoveFavorite = { uid -> viewModel.removeFromFavorites(uid) },
                                onCoinClick = { coinUid ->
                                    val arguments = CoinFragment.Input(coinUid, "market_watchlist")
                                    navController.slideFromRight(R.id.coinFragment, arguments)
                                },
                                preItems = {
                                    stickyHeader {
                                        MarketFavoritesMenu(
                                            sortDescending = data.sortingDescending,
                                            periodSelect = data.periodSelect,
                                            onSortingToggle = viewModel::onSortToggle,
                                            onSelectPeriod = viewModel::onSelectTimeDuration
                                        )
                                    }
                                }
                            )
                            if (scrollToTopAfterUpdate) {
                                scrollToTopAfterUpdate = false
                            }
                        }
                    }
                }

                null -> {}
            }
        }
    }

}

@Composable
fun MarketFavoritesMenu(
    sortDescending: Boolean,
    periodSelect: Select<Period>,
    onSortingToggle: () -> Unit,
    onSelectPeriod: (Period) -> Unit
) {

    HeaderSorting(borderTop = true, borderBottom = true) {
        Box(modifier = Modifier.weight(1f)) {
            ButtonSecondaryCircle(
                modifier = Modifier.padding(start = 16.dp),
                icon = if (sortDescending) R.drawable.ic_sort_h2l_20 else R.drawable.ic_sort_l2h_20,
                onClick = onSortingToggle
            )
        }
        ButtonSecondaryToggle(
            modifier = Modifier.padding(end = 16.dp),
            select = periodSelect,
            onSelect = onSelectPeriod
        )
    }
}

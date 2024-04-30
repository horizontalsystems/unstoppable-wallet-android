package io.horizontalsystems.bankwallet.modules.market.metricspage

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statPage
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Chart
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryToggle
import io.horizontalsystems.bankwallet.ui.compose.components.DescriptionCard
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderSorting
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.MarketCoinClear
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem

class MetricsPageFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val metricsType = navController.requireInput<MetricsType>()
        val factory = MetricsPageModule.Factory(metricsType)
        val chartViewModel by viewModels<ChartViewModel> { factory }
        val viewModel by viewModels<MetricsPageViewModel> { factory }
        MetricsPage(viewModel, chartViewModel, navController) {
            onCoinClick(it, navController)

            stat(page = metricsType.statPage, event = StatEvent.OpenCoin(it))
        }
    }

    private fun onCoinClick(coinUid: String, navController: NavController) {
        val arguments = CoinFragment.Input(coinUid)

        navController.slideFromRight(R.id.coinFragment, arguments)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MetricsPage(
        viewModel: MetricsPageViewModel,
        chartViewModel: ChartViewModel,
        navController: NavController,
        onCoinClick: (String) -> Unit,
    ) {
        val itemsViewState by viewModel.viewStateLiveData.observeAsState()
        val viewState = itemsViewState?.merge(chartViewModel.uiState.viewState)
        val marketData by viewModel.marketLiveData.observeAsState()
        val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)

        Column(Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = {
                            navController.popBackStack()
                        }
                    )
                )
            )

            HSSwipeRefresh(
                refreshing = isRefreshing,
                onRefresh = {
                    viewModel.refresh()
                }
            ) {
                Crossfade(viewState) { viewState ->
                    when (viewState) {
                        ViewState.Loading -> {
                            Loading()
                        }

                        is ViewState.Error -> {
                            ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                        }

                        ViewState.Success -> {
                            val listState = rememberSaveable(
                                marketData?.menu?.sortDescending,
                                saver = LazyListState.Saver
                            ) {
                                LazyListState()
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = listState,
                                contentPadding = PaddingValues(bottom = 32.dp),
                            ) {
                                item {
                                    viewModel.header.let { header ->
                                        DescriptionCard(header.title, header.description, header.icon)
                                    }
                                }
                                item {
                                    Chart(chartViewModel = chartViewModel)
                                }
                                marketData?.let { marketData ->
                                    stickyHeader {
                                        Menu(
                                            marketData.menu,
                                            viewModel::onToggleSortType,
                                            viewModel::onSelectMarketField
                                        )
                                    }
                                    items(marketData.marketViewItems) { marketViewItem ->
                                        MarketCoinClear(
                                            marketViewItem.fullCoin.coin.name,
                                            marketViewItem.fullCoin.coin.code,
                                            marketViewItem.fullCoin.coin.imageUrl,
                                            marketViewItem.fullCoin.iconPlaceholder,
                                            marketViewItem.coinRate,
                                            marketViewItem.marketDataValue,
                                            marketViewItem.rank,
                                        ) { onCoinClick(marketViewItem.fullCoin.coin.uid) }
                                    }
                                }
                            }
                        }

                        null -> {}
                    }
                }
            }
        }
    }

    @Composable
    private fun Menu(
        menu: MetricsPageModule.Menu,
        onToggleSortType: () -> Unit,
        onSelectMarketField: (MarketField) -> Unit
    ) {
        HeaderSorting(borderTop = true, borderBottom = true) {
            ButtonSecondaryCircle(
                modifier = Modifier
                    .padding(start = 16.dp),
                icon = if (menu.sortDescending) R.drawable.ic_sort_l2h_20 else R.drawable.ic_sort_h2l_20,
                onClick = { onToggleSortType() }
            )
            Spacer(Modifier.weight(1f))
            ButtonSecondaryToggle(
                modifier = Modifier.padding(end = 16.dp),
                select = menu.marketFieldSelect,
                onSelect = onSelectMarketField
            )
        }
    }
}

package cash.p.terminal.modules.market.overview

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.StatSection
import cash.p.terminal.core.stats.stat
import cash.p.terminal.core.stats.statPeriod
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.modules.market.overview.ui.MetricChartsView
import cash.p.terminal.modules.market.overview.ui.TopPairsBoardView
import cash.p.terminal.modules.market.overview.ui.TopPlatformsBoardView
import cash.p.terminal.modules.market.overview.ui.TopSectorsBoardView
import cash.p.terminal.ui.compose.HSSwipeRefresh
import cash.p.terminal.ui.compose.components.ListErrorView
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.helpers.LinkHelper

@Composable
fun MarketOverviewScreen(
    navController: NavController,
    viewModel: MarketOverviewViewModel = viewModel(factory = MarketOverviewModule.Factory())
) {
    val context = LocalContext.current
    val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
    val viewState by viewModel.viewStateLiveData.observeAsState()
    val viewItem by viewModel.viewItem.observeAsState()

    val scrollState = rememberScrollState()

    HSSwipeRefresh(
        refreshing = isRefreshing,
        onRefresh = {
            viewModel.refresh()

            stat(page = StatPage.MarketOverview, event = StatEvent.Refresh)
        }
    ) {
        Crossfade(viewState, label = "") { viewState ->
            when (viewState) {
                ViewState.Loading -> {
                    Loading()
                }
                is ViewState.Error -> {
                    ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                }
                ViewState.Success -> {
                    viewItem?.let { viewItem ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            MetricChartsView(viewItem.marketMetrics, navController)
//                            BoardsView(
//                                boards = viewItem.boards,
//                                navController = navController,
//                                onClickSeeAll = { listType ->
//                                    val (sortingField, topMarket, marketField) = viewModel.getTopCoinsParams(
//                                        listType
//                                    )
//
////                                    navController.slideFromBottom(
////                                        R.id.marketTopCoinsFragment,
////                                        MarketTopCoinsFragment.Input(
////                                            sortingField,
////                                            topMarket,
////                                            marketField
////                                        )
////                                    )
//
//                                    stat(page = StatPage.MarketOverview, section = listType.statSection, event = StatEvent.Open(StatPage.TopCoins))
//                                },
//                                onSelectTopMarket = { topMarket, listType ->
//                                    viewModel.onSelectTopMarket(topMarket, listType)
//
//                                    stat(page = StatPage.MarketOverview, section = listType.statSection, event = StatEvent.SwitchMarketTop(topMarket.statMarketTop))
//                                }
//                            )

                            TopPairsBoardView(
                                topMarketPairs = viewItem.topMarketPairs,
                                onItemClick = {
                                    it.tradeUrl?.let {
                                        LinkHelper.openLinkInAppBrowser(context, it)

                                        stat(page = StatPage.MarketOverview, event = StatEvent.Open(StatPage.ExternalMarketPair))
                                    }
                                }
                            ) {
                                //navController.slideFromBottom(R.id.topPairsFragment)

//                                stat(page = StatPage.MarketOverview, event = StatEvent.Open(StatPage.TopMarketPairs))
                            }

                            TopPlatformsBoardView(
                                viewItem.topPlatformsBoard,
                                onSelectTimeDuration = { timeDuration ->
                                    viewModel.onSelectTopPlatformsTimeDuration(timeDuration)

                                    stat(page = StatPage.MarketOverview, section = StatSection.TopPlatforms, event = StatEvent.SwitchPeriod(timeDuration.statPeriod))
                                },
                                onItemClick = {
                                    navController.slideFromRight(R.id.marketPlatformFragment, it)

                                    stat(page = StatPage.MarketOverview, event = StatEvent.OpenPlatform(it.uid))
                                },
                                onClickSeeAll = {
                                    val timeDuration = viewModel.topPlatformsTimeDuration

//                                    navController.slideFromBottom(
//                                        R.id.marketTopPlatformsFragment,
//                                        timeDuration
//                                    )

//                                    stat(page = StatPage.MarketOverview, event = StatEvent.Open(StatPage.TopPlatforms))
                                }
                            )

                            TopSectorsBoardView(
                                board = viewItem.topSectorsBoard
                            ) { coinCategory ->
                                navController.slideFromBottom(
                                    R.id.marketCategoryFragment,
                                    coinCategory
                                )

                                stat(page = StatPage.MarketOverview, event = StatEvent.OpenCategory(coinCategory.uid))
                            }

                            VSpacer(height = 32.dp)
                        }
                    }
                }
                null -> {}
            }
        }
    }
}

package io.horizontalsystems.bankwallet.modules.market.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.coin.overview.Loading
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.bankwallet.modules.market.metricspage.MetricsPageFragment
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.MarketMetrics
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.TimeDuration
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.TopNftCollectionViewItem
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.TopNftCollectionsBoard
import io.horizontalsystems.bankwallet.modules.market.topcoins.MarketTopCoinsFragment
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.MarketMetricSmallView
import io.horizontalsystems.bankwallet.ui.extensions.MetricData
import io.horizontalsystems.core.findNavController

class MarketOverviewFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    MarketOverviewScreen(findNavController())
                }
            }
        }
    }

}

@Composable
private fun MarketOverviewScreen(
    navController: NavController,
    viewModel: MarketOverviewViewModel = viewModel(factory = MarketOverviewModule.Factory())
) {
    val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
    val viewState by viewModel.viewStateLiveData.observeAsState()
    val viewItem by viewModel.viewItem.observeAsState()

    val scrollState = rememberScrollState()

    HSSwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = {
            viewModel.refresh()
        }
    ) {
        Crossfade(viewState) { viewState ->
            when (viewState) {
                is ViewState.Loading -> {
                    Loading()
                }
                is ViewState.Error -> {
                    ListErrorView(stringResource(R.string.SyncError), viewModel::onErrorClick)
                }
                is ViewState.Success -> {
                    viewItem?.let { viewItem ->
                        Column(
                            modifier = Modifier.verticalScroll(scrollState)
                        ) {
                            Box(
                                modifier = Modifier.height(240.dp)
                            ) {
                                MetricChartsView(viewItem.marketMetrics, navController)
                            }
                            BoardsView(
                                boards = viewItem.boards,
                                navController = navController,
                                onClickSeeAll = { listType ->
                                    val (sortingField, topMarket, marketField) = viewModel.getTopCoinsParams(
                                        listType
                                    )
                                    val args = MarketTopCoinsFragment.prepareParams(
                                        sortingField,
                                        topMarket,
                                        marketField
                                    )

                                    navController.slideFromBottom(R.id.marketTopCoinsFragment, args)
                                },
                                onSelectTopMarket = { topMarket, listType ->
                                    viewModel.onSelectTopMarket(topMarket, listType)
                                }
                            )

                            TopNftCollectionsBoardView(
                                viewItem.topNftCollectionsBoard,
                                onSelectTimeDuration = { timeDuration ->
                                    viewModel.onSelectTopNftsTimeDuration(timeDuration)
                                },
                                onClickSeeAll = {

                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopNftCollectionsBoardView(
    board: TopNftCollectionsBoard,
    onSelectTimeDuration: (TimeDuration) -> Unit,
    onClickSeeAll: () -> Unit
) {
    TopBoardHeader(
        title = board.title,
        iconRes = board.iconRes,
        select = board.timeDurationSelect,
        onSelect = onSelectTimeDuration
    )

    board.collections.forEachIndexed { index, collection ->
        TopNftCollectionView(collection, firstItem = index == 0)
    }

    SeeAllButton(onClickSeeAll)
}

@Composable
private fun TopNftCollectionView(
    collection: TopNftCollectionViewItem,
    firstItem: Boolean
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(getRoundedCornerShape(firstItem))
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        MultilineClear(
            onClick = { },
            borderBottom = true
        ) {
            CoinImage(
                iconUrl = collection.imageUrl ?: "",
                placeholder = R.drawable.coin_placeholder,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(24.dp)
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                MarketCoinFirstRow(collection.name, collection.volume)
                Spacer(modifier = Modifier.height(3.dp))
                MarketCoinSecondRow(
                    collection.floorPrice ?: "",
                    MarketDataValue.Diff(collection.volumeDiff),
                    "${collection.order}"
                )
            }
        }
    }
}

@Composable
private fun BoardsView(
    boards: List<MarketOverviewModule.Board>,
    navController: NavController,
    onClickSeeAll: (MarketModule.ListType) -> Unit,
    onSelectTopMarket: (TopMarket, MarketModule.ListType) -> Unit
) {
    boards.forEach { boardItem ->
        TopBoardHeader(
            title = boardItem.boardHeader.title,
            iconRes = boardItem.boardHeader.iconRes,
            select = boardItem.boardHeader.topMarketSelect,
            onSelect = { topMarket -> onSelectTopMarket(topMarket, boardItem.type) }
        )

        boardItem.marketViewItems.forEachIndexed { index, coin ->
            MarketCoinWithBackground(coin, index == 0, navController)
        }

        SeeAllButton {
            onClickSeeAll(boardItem.type)
        }
    }
}

@Composable
private fun <T : WithTranslatableTitle> TopBoardHeader(
    title: Int,
    iconRes: Int,
    select: Select<T>,
    onSelect: (T) -> Unit
) {
    Column {
        Divider(
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10
        )
        Row(
            modifier = Modifier.height(42.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier.padding(horizontal = 16.dp),
                painter = painterResource(iconRes),
                contentDescription = "Section Header Icon"
            )
            Text(
                text = stringResource(title),
                color = ComposeAppTheme.colors.oz,
                style = ComposeAppTheme.typography.body,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            ButtonSecondaryToggle(
                modifier = Modifier.padding(end = 16.dp),
                select = select,
                onSelect = onSelect
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun MarketCoinWithBackground(
    marketViewItem: MarketViewItem,
    firstItem: Boolean,
    navController: NavController
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(getRoundedCornerShape(firstItem))
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        MarketCoinClear(
            marketViewItem.coinName,
            marketViewItem.coinCode,
            marketViewItem.iconUrl,
            marketViewItem.iconPlaceHolder,
            marketViewItem.coinRate,
            marketViewItem.marketDataValue,
            marketViewItem.rank
        ) {
            onItemClick(marketViewItem, navController)
        }
    }
}

@Composable
private fun SeeAllButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.Market_SeeAll),
                color = ComposeAppTheme.colors.oz,
                style = ComposeAppTheme.typography.body,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            Image(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = "right arrow icon",
            )
        }
    }
}

@Composable
private fun RowScope.ChartView(metricsData: MetricData, navController: NavController) {
    AndroidView(
        modifier = Modifier.Companion
            .weight(1f)
            .height(104.dp)
            .clickable {
                openMetricsPage(metricsData.type, navController)
            },
        factory = { context ->
            MarketMetricSmallView(context).apply {
                setMetricData(metricsData)
            }
        },
        update = { it.setMetricData(metricsData) }
    )
}

@Composable
private fun MetricChartsView(marketMetrics: MarketMetrics, navController: NavController) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
    ) {
        Row {
            ChartView(marketMetrics.totalMarketCap, navController)
            Spacer(Modifier.width(8.dp))
            ChartView(marketMetrics.volume24h, navController)
        }
        Spacer(Modifier.height(8.dp))
        Row {
            ChartView(marketMetrics.defiCap, navController)
            Spacer(Modifier.width(8.dp))
            ChartView(marketMetrics.defiTvl, navController)
        }
    }
}

private fun onItemClick(marketViewItem: MarketViewItem, navController: NavController) {
    val arguments = CoinFragment.prepareParams(marketViewItem.coinUid)
    navController.slideFromRight(R.id.coinFragment, arguments)
}

private fun openMetricsPage(metricsType: MetricsType, navController: NavController) {
    if (metricsType == MetricsType.TvlInDefi) {
        navController.slideFromBottom(R.id.tvlFragment)
    } else {
        navController.slideFromBottom(
            R.id.metricsPageFragment,
            MetricsPageFragment.prepareParams(metricsType)
        )
    }
}

private fun getRoundedCornerShape(firstItem: Boolean): RoundedCornerShape {
    return if (firstItem) {
        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    } else {
        RoundedCornerShape(0.dp)
    }
}

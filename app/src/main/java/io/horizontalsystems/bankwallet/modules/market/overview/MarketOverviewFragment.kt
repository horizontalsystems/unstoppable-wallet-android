package io.horizontalsystems.bankwallet.modules.market.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.MarketViewModel
import io.horizontalsystems.bankwallet.modules.market.topcoins.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.getText
import io.horizontalsystems.bankwallet.modules.market.metricspage.MetricsPageFragment
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.MarketMetrics
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.ViewItemState
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryToggle
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.MarketListCoin
import io.horizontalsystems.bankwallet.ui.extensions.MarketMetricSmallView
import io.horizontalsystems.bankwallet.ui.extensions.MetricData
import io.horizontalsystems.core.findNavController

class MarketOverviewFragment : BaseFragment() {

    private val marketOverviewViewModel by viewModels<MarketOverviewViewModel> { MarketOverviewModule.Factory() }
    private val marketViewModel by navGraphViewModels<MarketViewModel>(R.id.mainFragment)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ComposeAppTheme {
                    MarketOverviewScreen()
                }
            }
        }
    }

    private fun onItemClick(marketViewItem: MarketViewItem) {
        val arguments = CoinFragment.prepareParams(marketViewItem.coinUid)

        findNavController().navigate(R.id.coinFragment, arguments, navOptions())
    }

    private fun openMetricsPage(metricsType: MetricsType) {
        val arguments = MetricsPageFragment.prepareParams(metricsType)
        findNavController().navigate(
            R.id.mainFragment_to_metricPageFragment,
            arguments,
            navOptions()
        )
    }

    @Composable
    private fun MarketOverviewScreen() {
        val loading by marketOverviewViewModel.loadingLiveData.observeAsState()
        val isRefreshing by marketOverviewViewModel.isRefreshingLiveData.observeAsState()
        val viewItemState by marketOverviewViewModel.viewItemStateLiveData.observeAsState()
        val scrollState = rememberScrollState()

        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing ?: false || loading ?: false),
            onRefresh = {
                marketOverviewViewModel.refresh()
            },
            indicator = { state, trigger ->
                SwipeRefreshIndicator(
                    state = state,
                    refreshTriggerDistance = trigger,
                    scale = true,
                    backgroundColor = ComposeAppTheme.colors.claude,
                    contentColor = ComposeAppTheme.colors.oz,
                )
            }
        ) {
            when (val state = viewItemState) {
                is ViewItemState.Error -> {
                    ListErrorView(
                        stringResource(R.string.Market_SyncError)
                    ) {
                        marketOverviewViewModel.onErrorClick()
                    }
                }
                is ViewItemState.Loaded -> {
                    Column(
                        modifier = Modifier
                            .verticalScroll(scrollState)
                    ) {
                        Box(
                            modifier = Modifier
                                .height(240.dp)
                        ) {
                            MetricChartsView(state.viewItem.marketMetrics)
                        }
                        BoardsView(state.viewItem.boards)
                    }
                }
            }
        }
    }

    @Composable
    private fun MetricChartsView(marketMetrics: MarketMetrics) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
        ) {
            Row {
                ChartView(marketMetrics.totalMarketCap)
                Spacer(Modifier.width(8.dp))
                ChartView(marketMetrics.volume24h)
            }
            Spacer(Modifier.height(8.dp))
            Row {
                ChartView(marketMetrics.defiCap)
                Spacer(Modifier.width(8.dp))
                ChartView(marketMetrics.defiTvl)
            }
        }
    }

    @Composable
    private fun RowScope.ChartView(metricsData: MetricData) {
        AndroidView(
            modifier = Modifier.Companion
                .weight(1f)
                .height(104.dp)
                .clickable {
                    openMetricsPage(metricsData.type)
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
    private fun BoardsView(boards: List<MarketOverviewModule.Board>) {
        boards.forEach { boardItem ->
            TopBoardHeader(boardItem)

            boardItem.marketViewItems.forEachIndexed { index, coin ->
                MarketCoin(coin, index == 0)
            }

            SeeAllButton(boardItem.type)
        }
    }

    @Composable
    private fun TopBoardHeader(board: MarketOverviewModule.Board) {
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
                    painter = painterResource(board.boardHeader.iconRes),
                    contentDescription = "Section Header Icon"
                )
                Text(
                    text = getString(board.boardHeader.title),
                    color = ComposeAppTheme.colors.oz,
                    style = ComposeAppTheme.typography.body,
                    maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                ButtonSecondaryToggle(
                    modifier = Modifier.padding(end = 16.dp),
                    toggleIndicators = board.boardHeader.toggleButton.indicators,
                    title = board.boardHeader.toggleButton.title,
                    onClick = { marketOverviewViewModel.onToggleTopBoardSize(board.type) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    @Composable
    private fun MarketCoin(marketViewItem: MarketViewItem, firstItem: Boolean) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(61.dp)
                .clip(getRoundedCornerShape(firstItem))
                .background(ComposeAppTheme.colors.lawrence)
        ) {
            MarketListCoin(
                marketViewItem.coinName,
                marketViewItem.coinCode,
                marketViewItem.rate,
                marketViewItem.iconUrl,
                marketViewItem.iconPlaceHolder,
                MarketDataValue.Diff(marketViewItem.diff),
                marketViewItem.score?.getText()
            ) {
                onItemClick(marketViewItem)
            }
        }
    }

    @Composable
    private fun SeeAllButton(listType: MarketModule.ListType) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                .background(ComposeAppTheme.colors.lawrence)
                .clickable { marketViewModel.onClickSeeAll(listType) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getString(R.string.Market_SeeAll),
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

    private fun getRoundedCornerShape(firstItem: Boolean): RoundedCornerShape {
        return if (firstItem) {
            RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        } else {
            RoundedCornerShape(0.dp)
        }
    }

}

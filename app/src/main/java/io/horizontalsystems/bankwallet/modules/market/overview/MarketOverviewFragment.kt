package io.horizontalsystems.bankwallet.modules.market.overview

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
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
import io.horizontalsystems.bankwallet.modules.market.getText
import io.horizontalsystems.bankwallet.modules.market.metrics.MarketMetrics
import io.horizontalsystems.bankwallet.modules.market.metrics.MarketMetricsModule
import io.horizontalsystems.bankwallet.modules.market.metrics.MarketMetricsViewModel
import io.horizontalsystems.bankwallet.modules.market.metricspage.MetricsPageFragment
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.RateColor
import io.horizontalsystems.bankwallet.ui.compose.RateText
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryToggle
import io.horizontalsystems.bankwallet.ui.extensions.MarketMetricSmallView
import io.horizontalsystems.bankwallet.ui.extensions.MetricData
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class MarketOverviewFragment : BaseFragment() {

    private val marketMetricsViewModel by viewModels<MarketMetricsViewModel> { MarketMetricsModule.Factory() }
    private val marketOverviewViewModel by viewModels<MarketOverviewViewModel> { MarketOverviewModule.Factory() }
    private val marketViewModel by navGraphViewModels<MarketViewModel>(R.id.mainFragment)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                ComposeAppTheme {
                    MarketOverviewScreen()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        marketMetricsViewModel.toastLiveData.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it)
        }

        marketOverviewViewModel.toastLiveData.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it)
        }
    }

    private fun onItemClick(marketViewItem: MarketViewItem) {
        val arguments = CoinFragment.prepareParams(marketViewItem.coinUid, marketViewItem.coinCode, marketViewItem.coinName)

        findNavController().navigate(R.id.coinFragment, arguments, navOptions())
    }

    private fun openMetricsPage(metricsType: MetricsType){
        val arguments = MetricsPageFragment.prepareParams(metricsType)
        findNavController().navigate(R.id.mainFragment_to_metricPageFragment, arguments, navOptions())
    }

    @Composable
    private fun MarketOverviewScreen() {
        val isRefreshing by marketOverviewViewModel.isRefreshing.observeAsState()
        val topBoardsState by marketOverviewViewModel.stateLiveData.observeAsState()
        val metricsData by marketMetricsViewModel.stateLiveData.observeAsState()

        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing ?: false),
            onRefresh = {
                marketOverviewViewModel.refresh()
                marketMetricsViewModel.refresh()
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
            LazyColumn {
                MetricCharts(metricsData)
                TopBoards(topBoardsState)
            }
        }

    }

    private fun LazyListScope.MetricCharts(state: MarketMetricsModule.State?) {
        state?.let {
            when (it) {
                MarketMetricsModule.State.Loading -> {
                    item {
                        LoadingView()
                    }
                }
                MarketMetricsModule.State.SyncError -> {
                    item {
                        ErrorView()
                    }
                }
                is MarketMetricsModule.State.Data -> {
                    item {
                        Box(modifier = Modifier.height(240.dp).fillMaxWidth()) {
                            MetricChartsView(it.marketMetrics)
                        }
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

    private fun LazyListScope.TopBoards(topBoardsState: MarketOverviewModule.State?) {
        topBoardsState?.let { state ->
            when (state) {
                MarketOverviewModule.State.Loading -> {
                    item {
                        LoadingView()
                    }
                }
                is MarketOverviewModule.State.Error -> {
                    item {
                        ErrorView()
                    }
                }
                is MarketOverviewModule.State.Data -> BoardsView(state.boards)
            }
        }
    }

    @Composable
    private fun LoadingView() {
        Box(modifier = Modifier.height(240.dp).fillMaxWidth()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(24.dp),
                color = ComposeAppTheme.colors.grey,
                strokeWidth = 2.dp,
            )
        }
    }

    @Composable
    private fun ErrorView() {
        Box(modifier = Modifier.height(240.dp).fillMaxWidth()) {
            Text(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp)
                    .clickable {
                        marketOverviewViewModel.onErrorClick()
                    },
                text = getString(R.string.BalanceSyncError_Title),
                color = ComposeAppTheme.colors.grey,
                style = ComposeAppTheme.typography.subhead2,
            )
        }
    }

    private fun LazyListScope.BoardsView(boardItems: List<MarketOverviewModule.BoardItem>) {
        val ctx = context ?: return

        boardItems.forEach { boardItem ->
            item {
                TopBoardHeader(boardItem)
            }

            itemsIndexed(boardItem.boardContent.marketViewItems) { index, coin ->
                MarketCoin(coin, index == 0, ctx)
            }

            item {
                SeeAllButton(boardItem.type)
            }
        }
    }

    @Composable
    private fun TopBoardHeader(boardItem: MarketOverviewModule.BoardItem) {
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
                    painter = painterResource(boardItem.boardHeader.iconRes),
                    contentDescription = "Section Header Icon"
                )
                Text(
                    text = getString(boardItem.boardHeader.title),
                    color = ComposeAppTheme.colors.oz,
                    style = ComposeAppTheme.typography.body,
                    maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                ButtonSecondaryToggle(
                    modifier = Modifier.padding(end = 16.dp),
                    toggleIndicators = boardItem.boardHeader.toggleButton.indicators,
                    title = boardItem.boardHeader.toggleButton.title,
                    onClick = { }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    @Composable
    private fun MarketCoin(marketViewItem: MarketViewItem, firstItem: Boolean, ctx: Context) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(61.dp)
                .clip(getRoundedCornerShape(firstItem))
                .background(ComposeAppTheme.colors.lawrence)
                .clickable { onItemClick(marketViewItem) }
        ) {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    // todo implement it
                    painter = painterResource(R.drawable.place_holder),
                    contentDescription = "coin icon",
                    modifier = Modifier.padding(horizontal = 16.dp).size(24.dp)
                )
                Column(
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    FirstRow(marketViewItem)
                    Spacer(modifier = Modifier.height(3.dp))
                    SecondRow(marketViewItem)
                }
            }
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
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
                modifier = Modifier.fillMaxHeight().padding(horizontal = 16.dp),
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

    @Composable
    private fun FirstRow(viewItem: MarketViewItem) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = viewItem.coinName,
                color = ComposeAppTheme.colors.oz,
                style = ComposeAppTheme.typography.body,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = viewItem.rate,
                color = ComposeAppTheme.colors.leah,
                style = ComposeAppTheme.typography.body,
                maxLines = 1,
            )
        }
    }

    @Composable
    private fun SecondRow(viewItem: MarketViewItem) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            viewItem.score?.let { score ->
                Box(
                    modifier = Modifier.padding(end = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ComposeAppTheme.colors.jeremy)
                ) {
                    Text(
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 1.dp),
                        text = score.getText(),
                        color = ComposeAppTheme.colors.bran,
                        style = ComposeAppTheme.typography.microSB,
                        maxLines = 1,
                    )
                }
            }
            Text(
                text = viewItem.coinName,
                color = ComposeAppTheme.colors.grey,
                style = ComposeAppTheme.typography.subhead2,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = RateText(viewItem.diff),
                color = RateColor(viewItem.diff),
                style = ComposeAppTheme.typography.subhead2,
                maxLines = 1,
            )
        }
    }

}

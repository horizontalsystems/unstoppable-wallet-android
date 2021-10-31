package io.horizontalsystems.bankwallet.modules.market.tvl

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinChartAdapter
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.ChartInfo
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.tvl.TvlModule.Diff
import io.horizontalsystems.bankwallet.modules.market.tvl.TvlModule.SelectorDialogState
import io.horizontalsystems.bankwallet.modules.market.tvl.TvlModule.SubtitleViewItem
import io.horizontalsystems.bankwallet.modules.market.tvl.TvlModule.ViewState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.findNavController

class TvlFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val viewModel by viewModels<TvlViewModel> { TvlModule.Factory() }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    TvlScreen(viewModel) { onCoinClick(it) }
                }
            }
        }
    }

    private fun onCoinClick(coinUid: String) {
        val arguments = CoinFragment.prepareParams(coinUid)

        findNavController().navigate(R.id.coinFragment, arguments, navOptions())
    }

    @Composable
    private fun TvlScreen(
        viewModel: TvlViewModel,
        onCoinClick: (String) -> Unit
    ) {
        val viewState by viewModel.viewStateLiveData.observeAsState()
        val chartData by viewModel.chartLiveData.observeAsState()
        val tvlData by viewModel.tvlLiveData.observeAsState()
        val loading by viewModel.loadingLiveData.observeAsState(false)
        val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)
        val chainSelectorDialogState by viewModel.chainSelectorDialogStateLiveData.observeAsState(SelectorDialogState.Closed)

        var scrollingEnabled by remember { mutableStateOf(true) }

        Column {
            AppBar(
                title = TranslatableString.ResString(R.string.MarketGlobalMetrics_TvlInDefi),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = {
                            findNavController().popBackStack()
                        }
                    )
                )
            )

            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing || loading),
                onRefresh = {
                    viewModel.refresh()
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
                when (viewState) {
                    ViewState.Error -> {
                        ListErrorView(
                            stringResource(R.string.Market_SyncError)
                        ) {
                            viewModel.onErrorClick()
                        }
                    }
                    ViewState.Success -> {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState(), enabled = scrollingEnabled)) {

                            chartData?.let { chartData ->
                                TvlSubtitle(chartData.subtitle)

                                ChartInfo(
                                    CoinChartAdapter.ViewItemWrapper(chartData.chartInfoData),
                                    chartData.currency,
                                    CoinChartAdapter.ChartViewType.MarketMetricChart,
                                    object : CoinChartAdapter.Listener {
                                        override fun onChartTouchDown() {
                                            scrollingEnabled = false
                                        }

                                        override fun onChartTouchUp() {
                                            scrollingEnabled = true
                                        }

                                        override fun onTabSelect(chartType: ChartView.ChartType) {
                                            viewModel.onSelectChartType(chartType)
                                        }
                                    })
                            }

                            tvlData?.let { tvlData ->
                                TvlMenu(
                                    tvlData.menu,
                                    viewModel::onClickChainSelector,
                                    viewModel::onToggleSortType,
                                    viewModel::onToggleTvlDiffType
                                )

                                CoinList(tvlData.coinTvlViewItems, onCoinClick)
                            }
                        }
                    }
                }
            }
            // chain selector dialog
            when (val option = chainSelectorDialogState) {
                is SelectorDialogState.Opened -> {
                    AlertGroup(
                        R.string.MarketGlobalMetrics_ChainSelectorTitle,
                        option.select,
                        viewModel::onSelectChain,
                        viewModel::onChainSelectorDialogDismiss
                    )
                }
            }
        }
    }

    @Composable
    private fun TvlMenu(
        menu: TvlModule.Menu,
        onClickChainSelector: () -> Unit,
        onToggleSortType: () -> Unit,
        onToggleTvlDiffType: () -> Unit
    ) {
        Header(borderBottom = true) {
            Box(modifier = Modifier.weight(1f)) {
                SortMenu(menu.chainSelect.selected.title) {
                    onClickChainSelector()
                }
            }
            ButtonSecondaryCircle(
                modifier = Modifier.padding(end = 16.dp),
                icon = if (menu.sortDescending) R.drawable.ic_arrow_down_20 else R.drawable.ic_arrow_up_20,
                onClick = { onToggleSortType() }
            )
            ButtonSecondaryCircle(
                modifier = Modifier.padding(end = 16.dp),
                icon = if (menu.tvlDiffType == TvlModule.TvlDiffType.Percent) R.drawable.ic_percent_20 else R.drawable.ic_usd_20,
                onClick = { onToggleTvlDiffType() }
            )
        }
    }

    @Composable
    private fun TvlSubtitle(item: SubtitleViewItem) {
        TabBalance {
            Text(
                modifier = Modifier.padding(end = 8.dp),
                text = item.value ?: "",
                style = ComposeAppTheme.typography.headline1,
                color = ComposeAppTheme.colors.leah
            )

            item.diff?.let { diff ->
                val color = when (diff) {
                    is Diff.Positive,
                    is Diff.NoDiff -> ComposeAppTheme.colors.remus
                    is Diff.Negative -> ComposeAppTheme.colors.lucian
                }
                Text(
                    text = diff.value,
                    style = ComposeAppTheme.typography.subhead1,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun CoinList(
    items: List<TvlModule.CoinTvlViewItem>,
    onCoinClick: (String) -> Unit
) {
    items.forEach { item ->
        MarketCoin(
            item.fullCoin.coin.name,
            item.fullCoin.coin.code,
            item.fullCoin.coin.iconUrl,
            item.fullCoin.iconPlaceholder,
            item.tvl,
            MarketDataValue.DiffNew(item.tvlDiff),
            item.rank
        ) { onCoinClick.invoke(item.fullCoin.coin.uid) }
    }
}

@Composable
private fun MarketCoin(
    coinName: String,
    coinCode: String,
    coinIconUrl: String,
    coinIconPlaceholder: Int,
    tvl: String,
    marketDataValue: MarketDataValue? = null,
    label: String? = null,
    onClick: (() -> Unit)? = null
) {
    MultilineClear(
        onClick = onClick,
        borderBottom = true
    ) {
        CoinImage(
            iconUrl = coinIconUrl,
            placeholder = coinIconPlaceholder,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp)
        )
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            MarketCoinFirstRow(coinName, tvl)
            Spacer(modifier = Modifier.height(3.dp))
            MarketCoinSecondRow(coinCode, marketDataValue, label)
        }
    }
}

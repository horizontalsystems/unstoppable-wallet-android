package io.horizontalsystems.bankwallet.modules.market.metricspage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinChartAdapter
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.ChartInfo
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.ChartInfoHeader
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.findNavController

class MetricsPageFragment : BaseFragment() {

    private val metricsType by lazy {
        requireArguments().getParcelable<MetricsType>(METRICS_TYPE_KEY)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel by viewModels<MetricsPageViewModel> { MetricsPageModule.Factory(metricsType!!) }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    MetricsPage(viewModel) { onCoinClick(it) }
                }
            }
        }
    }

    private fun onCoinClick(coinUid: String) {
        val arguments = CoinFragment.prepareParams(coinUid)

        findNavController().navigate(R.id.coinFragment, arguments, navOptions())
    }

    @Composable
    fun MetricsPage(viewModel: MetricsPageViewModel, onCoinClick: (String) -> Unit) {
        val viewState by viewModel.viewStateLiveData.observeAsState()
        val chartData by viewModel.chartLiveData.observeAsState()
        val marketData by viewModel.marketLiveData.observeAsState()
        val loading by viewModel.loadingLiveData.observeAsState(false)
        val isRefreshing by viewModel.isRefreshingLiveData.observeAsState(false)

        Column(Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(viewModel.metricsType.title),
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

            HSSwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing || loading),
                onRefresh = {
                    viewModel.refresh()
                }
            ) {
                when (viewState) {
                    is ViewState.Error -> {
                        ListErrorView(
                            stringResource(R.string.Market_SyncError)
                        ) {
                            viewModel.onErrorClick()
                        }
                    }
                    ViewState.Success -> {
                        LazyColumn {
                            chartData?.let { chartData ->
                                item {
                                    ChartInfoHeader(chartData.subtitle)

                                    ChartInfo(
                                        CoinChartAdapter.ViewItemWrapper(chartData.chartInfoData),
                                        chartData.currency,
                                        CoinChartAdapter.ChartViewType.MarketMetricChart,
                                        listOf(
                                            Pair(ChartView.ChartType.DAILY, R.string.CoinPage_TimeDuration_Day),
                                            Pair(ChartView.ChartType.WEEKLY, R.string.CoinPage_TimeDuration_Week),
                                            Pair(ChartView.ChartType.MONTHLY, R.string.CoinPage_TimeDuration_Month)
                                        ),
                                        object : CoinChartAdapter.Listener {
                                            override fun onChartTouchDown() = Unit
                                            override fun onChartTouchUp() = Unit

                                            override fun onTabSelect(chartType: ChartView.ChartType) {
                                                viewModel.onSelectChartType(chartType)
                                            }
                                        })
                                }
                            }

                            marketData?.let { marketData ->
                                item {
                                    Menu(
                                        marketData.menu,
                                        viewModel::onToggleSortType,
                                        viewModel::onSelectMarketField
                                    )
                                }
                                items(marketData.marketViewItems) { marketViewItem ->
                                    MarketCoin(
                                        marketViewItem.fullCoin.coin.name,
                                        marketViewItem.fullCoin.coin.code,
                                        marketViewItem.fullCoin.coin.iconUrl,
                                        marketViewItem.fullCoin.iconPlaceholder,
                                        marketViewItem.coinRate,
                                        marketViewItem.marketDataValue,
                                        marketViewItem.rank,
                                    ) { onCoinClick(marketViewItem.fullCoin.coin.uid) }
                                }
                            }
                        }
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
        Header(borderBottom = true) {
            ButtonSecondaryCircle(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                icon = if (menu.sortDescending) R.drawable.ic_arrow_down_20 else R.drawable.ic_arrow_up_20,
                onClick = { onToggleSortType() }
            )
            ButtonSecondaryToggle(
                modifier = Modifier.padding(end = 16.dp),
                select = menu.marketFieldSelect,
                onSelect = onSelectMarketField
            )
        }
    }

    companion object {
        private const val METRICS_TYPE_KEY = "metric_type"

        fun prepareParams(metricType: MetricsType): Bundle {
            return bundleOf(METRICS_TYPE_KEY to metricType)
        }
    }
}

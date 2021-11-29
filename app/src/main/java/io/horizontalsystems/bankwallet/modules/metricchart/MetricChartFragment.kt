package io.horizontalsystems.bankwallet.modules.metricchart

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinChartAdapter
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.ChartInfo
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.ChartInfoHeader
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment
import io.horizontalsystems.chartview.ChartView
import kotlinx.android.synthetic.main.fragment_market_global.*

class MetricChartFragment : BaseBottomSheetDialogFragment()
//    , Chart.Listener
{

    private val coinUid by lazy {
        requireArguments().getString((coinUidKey)) ?: ""
    }

    private val title by lazy {
        requireArguments().getString((titleKey)) ?: ""
    }

//    private val metricChartType by lazy {
//        requireArguments().getParcelable<MetricChartType>(METRICS_CHART_TYPE_KEY)
//    }

//    private val actions = listOf(
//        Pair(ChartView.ChartType.DAILY, R.string.CoinPage_TimeDuration_Day),
//        Pair(ChartView.ChartType.WEEKLY, R.string.CoinPage_TimeDuration_Week),
//        Pair(ChartView.ChartType.MONTHLY, R.string.CoinPage_TimeDuration_Month),
//    )

    private val viewModel by viewModels<MetricChartViewModel> { MetricChartModule.Factory(coinUid) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setContentView(R.layout.fragment_market_global)

        setTitle(getString(R.string.CoinPage_TotalVolume))
        setSubtitle(getString(R.string.MarketGlobalMetrics_Chart))
        setHeaderIcon(R.drawable.ic_chart_24)

        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        composeView.setContent {
            TradingVolumeChartScreen(viewModel, title)
        }

//        viewModel.description?.let {
//            descriptionText.text = getString(it)
//        }
//        chart.setListener(this)
//        setChartTabs()
//
//        viewModel.loadingLiveData.observe(viewLifecycleOwner, { loading ->
//            if (loading) {
//                chart.showSpinner()
//            } else {
//                chart.hideSpinner()
//            }
//        })
//
//        viewModel.chartViewItemLiveData.observe(viewLifecycleOwner, { chartViewItem ->
//            topValue.text = chartViewItem.lastValueWithDiff?.value
//            diffValue.setDiff(chartViewItem.lastValueWithDiff?.diff)
//
//            chartViewItem.chartData?.let {
//                chart.showChart()
//                rootView.post {
//                    chart.setData(it, chartViewItem.chartType, chartViewItem.maxValue, chartViewItem.minValue)
//                }
//            }
//
//        })
//
//        viewModel.selectedPointLiveData.observe(viewLifecycleOwner, { selectedPoint ->
//            pointInfoDate.text = selectedPoint.date
//            pointInfoValue.text = selectedPoint.value
//        })
//
//        viewModel.toastLiveData.observe(viewLifecycleOwner) {
//            HudHelper.showErrorMessage(this.requireView(), it)
//        }

    }

    @Composable
    private fun TradingVolumeChartScreen(
        viewModel: MetricChartViewModel,
        coinName: String
    ) {
        val chartData by viewModel.chartLiveData.observeAsState()
        val chartTypes by viewModel.chartTypes.observeAsState(listOf())

        ComposeAppTheme {
            Column {
                chartData?.let { chartData ->
                    ChartInfoHeader(chartData.subtitle)

                    ChartInfo(
                        CoinChartAdapter.ViewItemWrapper(chartData.chartInfoData),
                        chartData.currency,
                        CoinChartAdapter.ChartViewType.MarketMetricChart,
                        chartTypes,
                        object : CoinChartAdapter.Listener {
                            override fun onChartTouchDown() = Unit

                            override fun onChartTouchUp() = Unit

                            override fun onTabSelect(chartType: ChartView.ChartType) {
                                viewModel.onSelectChartType(chartType)
                            }
                        })
                }

                BottomSheetText(
                    text = stringResource(id = R.string.MarketGlobalMetrics_VolumeDescriptionCoin, coinName)
                )
                BottomSheetText(
                    text = stringResource(id = R.string.Market_PoweredByApi)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    @Composable
    private fun BottomSheetText(text: String) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 24.dp),
            text = text,
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead2
        )
    }

//    private fun setChartTabs() {
//        tabLayoutCompose.setViewCompositionStrategy(
//            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
//        )
//        tabLayoutCompose.setContent {
//            ComposeAppTheme {
//                CustomTab(actions.map { getString(it.second) })
//            }
//        }
//
//    }

//    @Composable
//    fun CustomTab(tabTitles: List<String>) {
//        var tabIndex by remember { mutableStateOf(0) }
//
//        LazyRow(modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)) {
//            itemsIndexed(tabTitles) { index, title ->
//                val selected = tabIndex == index
//                TabButtonSecondaryTransparent(
//                    title = title,
//                    onSelect = {
//                        tabIndex = index
//                        val chartType = actions[index].first
//                        viewModel.onChartTypeSelect(chartType)
//                    },
//                    selected = selected
//                )
//            }
//        }
//    }


//    //Chart.Listener
//
//    override fun onTouchDown() {
//        draggable = false
//        chartPointsInfo.isInvisible = false
//        tabLayoutCompose.isInvisible = true
//    }

//    override fun onTouchUp() {
//        draggable = true
//        chartPointsInfo.isInvisible = true
//        tabLayoutCompose.isInvisible = false
//    }

//    override fun onTouchSelect(point: PointInfo) {
//        viewModel.onTouchSelect(point)
//        HudHelper.vibrate(requireContext())
//    }

    companion object {
        private const val coinUidKey = "coinUidKey"
        private const val titleKey = "titleKey"

        fun show(fragmentManager: FragmentManager, coinUid: String, title: String) {
            val fragment = MetricChartFragment()
            fragment.arguments = bundleOf(coinUidKey to coinUid, titleKey to title)
            fragment.show(fragmentManager, "metric_chart_dialog")
        }
    }
}

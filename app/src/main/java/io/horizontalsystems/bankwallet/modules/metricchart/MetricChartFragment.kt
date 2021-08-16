package io.horizontalsystems.bankwallet.modules.metricchart

import android.os.Bundle
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.TabRounded
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment
import io.horizontalsystems.chartview.Chart
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_market_global.*

class MetricChartFragment : BaseBottomSheetDialogFragment(), Chart.Listener {

    private val metricChartType by lazy {
        requireArguments().getParcelable<MetricChartType>(METRICS_CHART_TYPE_KEY)
    }

    private val actions = listOf(
            Pair(ChartView.ChartType.DAILY, R.string.CoinPage_TimeDuration_Day),
            Pair(ChartView.ChartType.WEEKLY, R.string.CoinPage_TimeDuration_Week),
            Pair(ChartView.ChartType.MONTHLY, R.string.CoinPage_TimeDuration_Month),
    )

    private val viewModel by viewModels<MetricChartViewModel> { MetricChartModule.Factory(metricChartType) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setContentView(R.layout.fragment_market_global)

        setTitle(getString(viewModel.title))
        setSubtitle(getString(R.string.MarketGlobalMetrics_Chart))
        setHeaderIconDrawable(context?.let { AppCompatResources.getDrawable(it, R.drawable.ic_chart_24) })

        viewModel.description?.let {
            descriptionText.text = getString(it)
        }
        chart.setListener(this)
        setChartTabs()

        viewModel.loadingLiveData.observe(viewLifecycleOwner, { loading ->
            if (loading) {
                chart.showSpinner()
            } else {
                chart.hideSpinner()
            }
        })

        viewModel.chartViewItemLiveData.observe(viewLifecycleOwner, { chartViewItem ->
            topValue.text = chartViewItem.lastValueWithDiff?.value
            diffValue.setDiff(chartViewItem.lastValueWithDiff?.diff)

            chartViewItem.chartData?.let {
                chart.showChart()
                rootView.post {
                    chart.setData(it, chartViewItem.chartType, chartViewItem.maxValue, chartViewItem.minValue)
                }
            }

        })

        viewModel.selectedPointLiveData.observe(viewLifecycleOwner, { selectedPoint ->
            pointInfoDate.text = selectedPoint.date
            pointInfoValue.text = selectedPoint.value
        })

        viewModel.toastLiveData.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(this.requireView(), it)
        }

    }

    private fun setChartTabs() {
        tabLayoutCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )
        tabLayoutCompose.setContent {
            ComposeAppTheme {
                CustomTab(actions.map { getString(it.second) })
            }
        }

    }

    @Composable
    fun CustomTab(tabTitles: List<String>) {
        var tabIndex by remember { mutableStateOf(0) }

        LazyRow {
            itemsIndexed(tabTitles) { index, title ->
                val selected = tabIndex == index
                TabRounded(
                    title = title,
                    onSelect = {
                        tabIndex = index
                        val chartType = actions[index].first
                        viewModel.onChartTypeSelect(chartType)
                    },
                    selected = selected
                )
            }
        }
    }


    //Chart.Listener

    override fun onTouchDown() {
        draggable = false
        chartPointsInfo.isInvisible = false
        tabLayoutCompose.isInvisible = true
    }

    override fun onTouchUp() {
        draggable = true
        chartPointsInfo.isInvisible = true
        tabLayoutCompose.isInvisible = false
    }

    override fun onTouchSelect(point: PointInfo) {
        viewModel.onTouchSelect(point)
        HudHelper.vibrate(requireContext())
    }

    companion object {
        private const val METRICS_CHART_TYPE_KEY = "metric_chart_type"

        fun show(fragmentManager: FragmentManager, metricChartType: MetricChartType) {
            val fragment = MetricChartFragment()
            fragment.arguments = bundleOf(METRICS_CHART_TYPE_KEY to metricChartType)
            fragment.show(fragmentManager, "metric_chart_dialog")
        }
    }
}

package io.horizontalsystems.bankwallet.modules.market.overview.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.market.metricspage.MetricsPageFragment
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.bankwallet.ui.extensions.MarketMetricSmallView
import io.horizontalsystems.bankwallet.ui.extensions.MetricData

@Composable
fun MetricChartsView(marketMetrics: MarketOverviewModule.MarketMetrics, navController: NavController) {
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

@Composable
private fun RowScope.ChartView(metricsData: MetricData, navController: NavController) {
    AndroidView(
        modifier = Modifier
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

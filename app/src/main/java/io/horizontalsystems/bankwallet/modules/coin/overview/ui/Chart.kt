package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.chartview.Chart
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.chartview.models.ChartIndicator
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.core.helpers.HudHelper
import java.util.*

@Composable
fun HsChartLineHeader(currentValue: String?, currentValueDiff: Value.Percent?) {
    TabBalance(borderTop = true) {
        Text(
            modifier = Modifier.padding(end = 8.dp),
            text = currentValue ?: "--",
            style = ComposeAppTheme.typography.headline1,
            color = ComposeAppTheme.colors.leah
        )

        currentValueDiff?.let {
            Text(
                text = formatValueAsDiff(it),
                style = ComposeAppTheme.typography.subhead1,
                color = diffColor(it.raw())
            )
        }
    }
}

@Composable
fun Chart(chartViewModel: ChartViewModel, onSelectChartType: ((ChartView.ChartType) -> Unit)? = null) {
    val chartDataWrapper by chartViewModel.dataWrapperLiveData.observeAsState()
    val chartTabs by chartViewModel.tabItemsLiveData.observeAsState(listOf())
    val chartIndicators by chartViewModel.indicatorsLiveData.observeAsState(listOf())
    val chartLoading by chartViewModel.loadingLiveData.observeAsState(false)
    val chartViewState by chartViewModel.viewStateLiveData.observeAsState()
    val currency = chartViewModel.currency

    Column {
        HsChartLineHeader(chartDataWrapper?.currentValue, chartDataWrapper?.currentValueDiff)
        Chart(
            tabItems = chartTabs,
            onSelectTab = {
                chartViewModel.onSelectChartType(it)
                onSelectChartType?.invoke(it)
            },
            indicators = chartIndicators,
            onSelectIndicator = {
                chartViewModel.onSelectIndicator(it)
            },
            chartInfoData = chartDataWrapper?.chartInfoData,
            chartLoading = chartLoading,
            viewState = chartViewState,
            currency = currency
        )
    }
}

@Composable
fun <T> Chart(
    tabItems: List<TabItem<T>>,
    onSelectTab: (T) -> Unit,
    indicators: List<TabItem<ChartIndicator>>,
    onSelectIndicator: (ChartIndicator?) -> Unit,
    chartInfoData: ChartInfoData?,
    chartLoading: Boolean,
    viewState: ViewState?,
    currency: Currency,
) {
    Column {
        var pointInfo by remember { mutableStateOf<PointInfo?>(null) }
        HsChartLinePeriodsAndPoint(tabItems, pointInfo, currency, onSelectTab)
        val chartIndicator = indicators.firstOrNull { it.selected }?.item
        PriceVolChart(chartInfoData, chartIndicator, chartLoading, viewState) {
            pointInfo = it
        }
        if (indicators.isNotEmpty()) {
            IndicatorToggles(indicators) {
                onSelectIndicator.invoke(it)
            }
        }
    }
}

@Composable
private fun <T> HsChartLinePeriodsAndPoint(
    tabItems: List<TabItem<T>>,
    pointInfo: PointInfo?,
    currency: Currency,
    onSelectTab: (T) -> Unit,
) {
    if (pointInfo == null) {
        ChartTab(tabItems, onSelectTab)
    } else {
        val value = App.numberFormatter.formatCurrencyValueAsShortened(CurrencyValue(currency, pointInfo.value.toBigDecimal()))
        val dayAndTime = DateHelper.getDayAndTime(Date(pointInfo.timestamp * 1000))

        TabPeriod(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column {
                Text(
                    text = value,
                    style = ComposeAppTheme.typography.captionSB,
                    color = ComposeAppTheme.colors.leah
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dayAndTime,
                    style = ComposeAppTheme.typography.caption,
                    color = ComposeAppTheme.colors.grey
                )
            }
            pointInfo.volume?.let { volume ->
                Column {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.CoinPage_Volume),
                        style = ComposeAppTheme.typography.caption,
                        color = ComposeAppTheme.colors.grey,
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = App.numberFormatter.formatCurrencyValueAsShortened(CurrencyValue(currency, volume.toBigDecimal())),
                        style = ComposeAppTheme.typography.caption,
                        color = ComposeAppTheme.colors.grey,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
fun IndicatorToggles(indicators: List<TabItem<ChartIndicator>>, onSelect: (ChartIndicator?) -> Unit) {
    CellHeaderSorting(
        borderTop = true,
        borderBottom = true
    ) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            indicators.forEach { indicator ->
                TabButtonSecondary(
                    title = indicator.title,
                    onSelect = {
                        onSelect(if (indicator.selected) null else indicator.item)
                    },
                    selected = indicator.selected,
                    enabled = true
                )
            }
        }
    }
}

@Composable
fun PriceVolChart(
    chartInfoData: ChartInfoData?,
    chartIndicator: ChartIndicator?,
    loading: Boolean,
    viewState: ViewState?,
    onSelectPoint: (PointInfo?) -> Unit,
) {
    Box(
        modifier = Modifier
            .height(182.dp)
    ) {
        Divider(thickness = 1.dp, color = ComposeAppTheme.colors.steel10)

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                Chart(it).apply {
                    setListener(object : Chart.Listener {
                        override fun onTouchDown() {
                        }

                        override fun onTouchUp() {
                            onSelectPoint.invoke(null)
                        }

                        override fun onTouchSelect(point: PointInfo) {
                            onSelectPoint.invoke(point)
                            HudHelper.vibrate(context)
                        }
                    })
                }
            },
            update = { chart ->
                if (loading) {
                    chart.showSpinner()
                } else {
                    chart.hideSpinner()
                }

                when (viewState) {
                    is ViewState.Error -> {
                        chart.showError(viewState.t.localizedMessage ?: "")
                    }
                    ViewState.Success -> {
                        chartInfoData?.let {
                            chart.post {
                                chart.setData(it.chartData, it.chartType, it.maxValue, it.minValue)
                                if (chartIndicator != null) {
                                    chart.setIndicator(chartIndicator, true)
                                } else {
                                    chart.hideAllIndicators()
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun <T> ChartTab(tabItems: List<TabItem<T>>, onSelect: (T) -> Unit) {
    val tabIndex = tabItems.indexOfFirst { it.selected }

    TabPeriod {
        ScrollableTabRow(
            selectedTabIndex = tabIndex,
            modifier = Modifier,
            backgroundColor = Color.Transparent,
            edgePadding = 0.dp,
            indicator = {},
            divider = {}
        ) {
            tabItems.forEachIndexed { index, tabItem ->
                val selected = tabIndex == index

                Tab(
                    selected = selected,
                    onClick = { },
                ) {
                    TabButtonSecondaryTransparent(
                        title = tabItem.title,
                        onSelect = {
                            onSelect.invoke(tabItem.item)
                        },
                        selected = selected
                    )
                }
            }
        }
    }
}


@Preview
@Composable
fun ChartPreview() {
    ComposeAppTheme {
        val tabItems = listOf(
            TabItem("TDY", true, ""),
            TabItem("24H", false, ""),
            TabItem("7D", false, ""),
            TabItem("1M", false, ""),
        )

//        Chart(
//            tabItems = tabItems,
//            indicators = indicators,
//            onSelectTab = {
//
//            },
//            onSelectIndicator = {
//
//            }
//        )
    }
}
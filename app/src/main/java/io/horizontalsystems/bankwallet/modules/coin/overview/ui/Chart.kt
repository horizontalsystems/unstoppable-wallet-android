package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.modules.chart.SelectedPoint
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.chartview.Chart
import io.horizontalsystems.chartview.ChartDataItemImmutable
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.chartview.models.ChartIndicator
import io.horizontalsystems.core.helpers.HudHelper

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
fun Chart(chartViewModel: ChartViewModel, onChangeHoldingPointState: (Boolean) -> Unit = {}, onSelectChartType: ((ChartView.ChartType) -> Unit)? = null) {
    val chartDataWrapper by chartViewModel.dataWrapperLiveData.observeAsState()
    val chartTabs by chartViewModel.tabItemsLiveData.observeAsState(listOf())
    val chartIndicators by chartViewModel.indicatorsLiveData.observeAsState(listOf())
    val chartLoading by chartViewModel.loadingLiveData.observeAsState(false)
    val chartViewState by chartViewModel.viewStateLiveData.observeAsState()

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
            itemToPointConverter = chartViewModel::getSelectedPoint,
            onChangeHoldingPointState = onChangeHoldingPointState
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
    itemToPointConverter: (ChartDataItemImmutable) -> SelectedPoint?,
    onChangeHoldingPointState: (Boolean) -> Unit
) {
    Column {
        var selectedPoint by remember { mutableStateOf<SelectedPoint?>(null) }
        HsChartLinePeriodsAndPoint(tabItems, selectedPoint, onSelectTab)
        val chartIndicator = indicators.firstOrNull { it.selected && it.enabled }?.item
        PriceVolChart(chartInfoData, chartIndicator, chartLoading, viewState) { item ->
            onChangeHoldingPointState.invoke(item != null)
            selectedPoint = item?.let { itemToPointConverter.invoke(it) }
        }
        if (indicators.isNotEmpty()) {
            HSIndicatorToggles(indicators) {
                onSelectIndicator.invoke(it)
            }
        }
    }
}

@Composable
private fun <T> HsChartLinePeriodsAndPoint(
    tabItems: List<TabItem<T>>,
    selectedPoint: SelectedPoint?,
    onSelectTab: (T) -> Unit,
) {
    Box {
        ChartTab(tabItems, onSelectTab)

        if (selectedPoint != null) {
            TabPeriod(
                modifier = Modifier
                    .background(ComposeAppTheme.colors.tyler)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = selectedPoint.value,
                        style = ComposeAppTheme.typography.captionSB,
                        color = ComposeAppTheme.colors.leah
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedPoint.date,
                        style = ComposeAppTheme.typography.caption,
                        color = ComposeAppTheme.colors.grey
                    )
                }

                when (val extraData = selectedPoint.extraData) {
                    is SelectedPoint.ExtraData.Macd -> {
                        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                            extraData.histogram?.let {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = extraData.histogram,
                                    style = ComposeAppTheme.typography.caption,
                                    color = ComposeAppTheme.colors.lucian,
                                    textAlign = TextAlign.End
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                extraData.macd?.let {
                                    Text(
                                        text = it,
                                        style = ComposeAppTheme.typography.caption,
                                        color = ComposeAppTheme.colors.issykBlue,
                                        textAlign = TextAlign.End
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                extraData.signal?.let {
                                    Text(
                                        text = it,
                                        style = ComposeAppTheme.typography.caption,
                                        color = ComposeAppTheme.colors.jacob,
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                    is SelectedPoint.ExtraData.Volume -> {
                        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
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
                                text = extraData.volume,
                                style = ComposeAppTheme.typography.caption,
                                color = ComposeAppTheme.colors.grey,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HSIndicatorToggles(indicators: List<TabItem<ChartIndicator>>, onSelect: (ChartIndicator?) -> Unit) {
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
                    enabled = indicator.enabled
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
    onSelectPoint: (ChartDataItemImmutable?) -> Unit,
) {
    Box(
        modifier = Modifier
            .height(182.dp)
    ) {
        Divider(thickness = 1.dp, color = ComposeAppTheme.colors.steel10)

        val coroutineScope = rememberCoroutineScope()

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

                        override fun onTouchSelect(item: ChartDataItemImmutable) {
                            onSelectPoint.invoke(item)
                            HudHelper.vibrate(context)
                        }
                    })
                }
            },
            update = { chart ->
                Log.e("AAA", "loading: $loading")
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

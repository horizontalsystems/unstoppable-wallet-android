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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
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
import io.horizontalsystems.marketkit.models.HsTimePeriod

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
fun Chart(chartViewModel: ChartViewModel, onSelectChartInterval: ((HsTimePeriod) -> Unit)? = null) {
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
                chartViewModel.onSelectChartInterval(it)
                onSelectChartInterval?.invoke(it)
            },
            indicators = chartIndicators,
            onSelectIndicator = {
                chartViewModel.onSelectIndicator(it)
            },
            chartInfoData = chartDataWrapper?.chartInfoData,
            chartLoading = chartLoading,
            viewState = chartViewState,
            itemToPointConverter = chartViewModel::getSelectedPoint
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
    itemToPointConverter: (ChartDataItemImmutable) -> SelectedPoint?
) {
    Column {
        var selectedPoint by remember { mutableStateOf<SelectedPoint?>(null) }
        HsChartLinePeriodsAndPoint(tabItems, selectedPoint, onSelectTab)
        val chartIndicator = indicators.firstOrNull { it.selected && it.enabled }?.item
        PriceVolChart(
            chartInfoData = chartInfoData,
            chartIndicator = chartIndicator,
            loading = chartLoading,
            viewState = viewState,
            showIndicatorLine = indicators.isNotEmpty()
        ) { item ->
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
        // Hide ChartTab if point is selected.
        // Simply hiding and showing makes period tabs shows up with scrolling animation
        // The desired behavior is to show without any animation
        // Solved it with alpha property
        val alpha = if (selectedPoint != null) 0f else 1f
        ChartTab(
            modifier = Modifier.alpha(alpha),
            tabItems = tabItems,
            onSelect = onSelectTab
        )

        if (selectedPoint != null) {
            TabPeriod(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    captionSB_leah(text = selectedPoint.value)
                    Spacer(modifier = Modifier.height(4.dp))
                    caption_grey(text = selectedPoint.date)
                }

                when (val extraData = selectedPoint.extraData) {
                    is SelectedPoint.ExtraData.Macd -> {
                        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                            extraData.histogram?.let {
                                caption_lucian(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = extraData.histogram,
                                    textAlign = TextAlign.End
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                extraData.macd?.let {
                                    caption_issykBlue(
                                        text = it,
                                        textAlign = TextAlign.End
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                extraData.signal?.let {
                                    caption_jacob(
                                        text = it,
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                    is SelectedPoint.ExtraData.Volume -> {
                        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                            caption_grey(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(R.string.CoinPage_Volume),
                                textAlign = TextAlign.End
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            caption_grey(
                                modifier = Modifier.fillMaxWidth(),
                                text = extraData.volume,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                    is SelectedPoint.ExtraData.Dominance -> {
                        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                            caption_grey(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(R.string.Market_BtcDominance),
                                textAlign = TextAlign.End
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            caption_jacob(
                                modifier = Modifier.fillMaxWidth(),
                                text = extraData.dominance,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                    null ->{}
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
    showIndicatorLine: Boolean,
    onSelectPoint: (ChartDataItemImmutable?) -> Unit,
) {
    val height = if (showIndicatorLine) 228.dp else 180.dp
    Box(
        modifier = Modifier
            .height(height)
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

                        override fun onTouchSelect(item: ChartDataItemImmutable) {
                            onSelectPoint.invoke(item)
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
                        chart.hideError()
                        chart.setIndicatorLineVisible(showIndicatorLine)

                        chartInfoData?.let { chartInfoData ->
                            val chartType = ChartView.ChartType.fromString(chartInfoData.chartInterval.value)
                            chart.doOnLayout {
                                chart.setData(chartInfoData.chartData, chartType, chartInfoData.maxValue, chartInfoData.minValue)
                                if (chartIndicator != null) {
                                    chart.setIndicator(chartIndicator, true)
                                } else {
                                    chart.hideAllIndicators()
                                }
                            }
                        }
                    }
                    ViewState.Loading,
                    null -> {}
                }
            }
        )
    }
}

@Composable
fun <T> ChartTab(modifier: Modifier = Modifier, tabItems: List<TabItem<T>>, onSelect: (T) -> Unit) {
    val tabIndex = tabItems.indexOfFirst { it.selected }

    TabPeriod(modifier = modifier) {
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

package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
import io.horizontalsystems.bankwallet.modules.chart.ChartModule
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.modules.chart.SelectedPoint
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.chartview.Chart
import io.horizontalsystems.chartview.ChartDataItemImmutable
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.HsTimePeriod

@Composable
fun HsChartLineHeader(chartHeaderView: ChartModule.ChartHeaderView?) {
    TabBalance(borderTop = true) {
        Text(
            modifier = Modifier.padding(end = 8.dp),
            text = chartHeaderView?.value ?: "--",
            style = ComposeAppTheme.typography.headline1,
            color = ComposeAppTheme.colors.leah
        )
        (chartHeaderView as? ChartModule.ChartHeaderView.Latest)?.let { latest ->
            Text(
                text = formatValueAsDiff(latest.diff),
                style = ComposeAppTheme.typography.subhead1,
                color = diffColor(latest.diff.raw())
            )
        }
    }
}

@Composable
fun Chart(chartViewModel: ChartViewModel, onSelectChartInterval: ((HsTimePeriod?) -> Unit)? = null) {
    val chartDataWrapper by chartViewModel.dataWrapperLiveData.observeAsState()
    val chartTabs by chartViewModel.tabItemsLiveData.observeAsState(listOf())
    val chartLoading by chartViewModel.loadingLiveData.observeAsState(false)
    val chartViewState by chartViewModel.viewStateLiveData.observeAsState()

    Column {
        HsChartLineHeader(chartDataWrapper?.chartHeaderView)
        Chart(
            tabItems = chartTabs,
            onSelectTab = {
                chartViewModel.onSelectChartInterval(it)
                onSelectChartInterval?.invoke(it)
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
    chartInfoData: ChartInfoData?,
    chartLoading: Boolean,
    viewState: ViewState?,
    itemToPointConverter: (ChartDataItemImmutable) -> SelectedPoint?
) {
    Column {
        var selectedPoint by remember { mutableStateOf<SelectedPoint?>(null) }
        HsChartLinePeriodsAndPoint(tabItems, selectedPoint, onSelectTab)
        PriceVolChart(
            chartInfoData = chartInfoData,
            loading = chartLoading,
            viewState = viewState
        ) { item ->
            selectedPoint = item?.let { itemToPointConverter.invoke(it) }
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
fun PriceVolChart(
    chartInfoData: ChartInfoData?,
    loading: Boolean,
    viewState: ViewState?,
    onSelectPoint: (ChartDataItemImmutable?) -> Unit,
) {
    val showIndicatorLine = chartInfoData?.hasVolumes ?: false
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
                            chart.doOnLayout {
                                chart.setData(chartInfoData.chartData, chartInfoData.maxValue, chartInfoData.minValue)
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

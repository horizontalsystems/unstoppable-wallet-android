package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.chartview.Chart
import io.horizontalsystems.chartview.models.PointInfo

//@Composable
//fun <T>Chart(
//    tabItems: List<TabItem<T>>,
//    indicators: List<IndicatorItem>,
//    chartInfoData: ChartInfoData?,
//    onSelectTab: (T) -> Unit,
//    onSelectIndicator: (IndicatorItem) -> Unit
//) {
//    Column {
//        ChartTab(tabItems, onSelectTab)
//        PriceVolChart(chartInfoData)
//        IndicatorToggles(indicators, onSelectIndicator)
//    }
//}

@Composable
fun <T>Chart(
    tabItems: List<TabItem<T>>,
    onSelectTab: (T) -> Unit,
    chartInfoData: ChartInfoData?,
    chartLoading: Boolean,
    viewState: ViewState?
) {
    Column {
//        if (showPointInfo) {
//            pointXxx?.let {
//                TabPeriod(modifier = Modifier.padding(horizontal = 16.dp)) {
//                    Column {
//                        Text(
//                            text = it.value,
//                            style = ComposeAppTheme.typography.captionSB,
//                            color = ComposeAppTheme.colors.leah
//                        )
//                        Spacer(modifier = Modifier.height(4.dp))
//                        Text(
//                            text = it.date,
//                            style = ComposeAppTheme.typography.caption,
//                            color = ComposeAppTheme.colors.grey
//                        )
//                    }
//                }
//            }
//        } else {
            ChartTab(tabItems, onSelectTab)
//        }


        PriceVolChart(chartInfoData, chartLoading, viewState)
    }
}

data class IndicatorItem(val title: String, val enabled: Boolean, val selected: Boolean)

@Composable
fun IndicatorToggles(indicators: List<IndicatorItem>, onSelect: (IndicatorItem) -> Unit) {
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
                        onSelect(indicator)
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
    loading: Boolean,
    viewState: ViewState?
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
                        }

                        override fun onTouchSelect(point: PointInfo) {
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
                            chart.setData(it.chartData, it.chartType, it.maxValue, it.minValue)
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

        val indicators = listOf(
            IndicatorItem("EMA", true, false),
            IndicatorItem("MACD", true, false),
            IndicatorItem("RSI", true, true),
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
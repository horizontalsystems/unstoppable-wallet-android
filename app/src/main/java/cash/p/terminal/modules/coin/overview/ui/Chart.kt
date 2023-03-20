package cash.p.terminal.modules.coin.overview.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import cash.p.terminal.R
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.chart.ChartModule
import cash.p.terminal.modules.chart.ChartViewModel
import cash.p.terminal.modules.coin.ChartInfoData
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.*
import io.horizontalsystems.chartview.Chart
import io.horizontalsystems.chartview.ChartDataItemImmutable
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.HsTimePeriod

@Composable
fun HsChartLineHeader(
    chartHeaderView: ChartModule.ChartHeaderView?,
) {
    val mainValue = chartHeaderView?.value ?: "--"
    val diff = chartHeaderView?.diff
    val date = chartHeaderView?.date
    val extraData = chartHeaderView?.extraData

    RowUniversal(
        modifier = Modifier
            .height(64.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            Row(
                modifier = Modifier.weight(1f),
            ) {
                val style = if (date == null) {
                    ComposeAppTheme.typography.title3
                } else {
                    ComposeAppTheme.typography.headline2
                }

                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = mainValue,
                    style = style,
                    color = ComposeAppTheme.colors.leah,
                )
                diff?.let {
                    HSpacer(width = 4.dp)
                    Text(
                        modifier = Modifier.alignByBaseline(),
                        text = formatValueAsDiff(diff),
                        style = ComposeAppTheme.typography.subhead1,
                        color = diffColor(diff.raw())
                    )
                }
            }

            date?.let {
                VSpacer(height = 1.dp)
                subhead2_grey(text = date)
            }
        }

        extraData?.let {
            Spacer(modifier = Modifier.weight(1f))
            when (extraData) {
                is ChartModule.ChartHeaderExtraData.Volume -> {
                    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                        subhead2_grey(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.CoinPage_Volume),
                            textAlign = TextAlign.End
                        )
                        subhead2_grey(
                            modifier = Modifier.fillMaxWidth(),
                            text = extraData.volume,
                            textAlign = TextAlign.End
                        )
                    }
                }
                is ChartModule.ChartHeaderExtraData.Dominance -> {
                    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                        subhead2_grey(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.Market_BtcDominance),
                            textAlign = TextAlign.End
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            subhead2_jacob(
                                text = extraData.dominance
                            )
                            extraData.diff?.let { diff ->
                                HSpacer(width = 4.dp)
                                Text(
                                    text = formatValueAsDiff(diff),
                                    style = ComposeAppTheme.typography.subhead2,
                                    color = diffColor(diff.raw())
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Chart(
    chartViewModel: ChartViewModel,
    onSelectChartInterval: ((HsTimePeriod?) -> Unit)? = null
) {
    val uiState = chartViewModel.uiState

    Column {
        var selectedPoint by remember { mutableStateOf<ChartModule.ChartHeaderView?>(null) }

        HsChartLineHeader(selectedPoint ?: uiState.chartHeaderView)
        Chart(
            tabItems = uiState.tabItems,
            onSelectTab = {
                chartViewModel.onSelectChartInterval(it)
                onSelectChartInterval?.invoke(it)
            },
            chartInfoData = uiState.chartInfoData,
            chartLoading = uiState.loading,
            viewState = uiState.viewState,
            hasVolumes = uiState.hasVolumes,
            chartViewType = uiState.chartViewType
        ) { item ->
            selectedPoint = item?.let {
                chartViewModel.getSelectedPoint(it)
            }
        }
    }
}

@Composable
fun <T> Chart(
    tabItems: List<TabItem<T>>,
    onSelectTab: (T) -> Unit,
    chartInfoData: ChartInfoData?,
    chartLoading: Boolean,
    viewState: ViewState?,
    hasVolumes: Boolean,
    chartViewType: ChartViewType,
    onSelectPoint: (ChartDataItemImmutable?) -> Unit,
) {
    Column {
        PriceVolChart(
            chartInfoData = chartInfoData,
            loading = chartLoading,
            viewState = viewState,
            onSelectPoint = onSelectPoint,
            hasVolumes = hasVolumes,
            chartViewType = chartViewType
        )
        VSpacer(height = 8.dp)
        ChartTab(
            tabItems = tabItems,
            onSelect = onSelectTab
        )
    }
}

@Composable
fun PriceVolChart(
    chartInfoData: ChartInfoData?,
    loading: Boolean,
    viewState: ViewState?,
    onSelectPoint: (ChartDataItemImmutable?) -> Unit,
    hasVolumes: Boolean,
    chartViewType: ChartViewType,
) {
    val height = if (hasVolumes) 204.dp else 160.dp

    AndroidView(
        modifier = Modifier
            .height(height)
            .fillMaxWidth(),
        factory = {
            Chart(it).apply {
                this.chartViewType = chartViewType

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
                    chart.setIndicatorLineVisible(hasVolumes)

                    chartInfoData?.let { chartInfoData ->
                        chart.doOnLayout {
                            chart.setData(
                                chartInfoData.chartData,
                                chartInfoData.maxValue,
                                chartInfoData.minValue
                            )
                        }
                    }
                }
                ViewState.Loading,
                null -> {
                }
            }
        }
    )
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
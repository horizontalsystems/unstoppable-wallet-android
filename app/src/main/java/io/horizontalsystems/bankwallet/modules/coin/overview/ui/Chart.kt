package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.chart.ChartModule
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.chartview.Chart
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.HsTimePeriod

@Composable
fun HsChartLineHeader(
    chartHeaderView: ChartModule.ChartHeaderView?,
) {
    val mainValue = chartHeaderView?.value ?: "--"
    val mainValueHint = chartHeaderView?.valueHint
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
                mainValueHint?.let {
                    HSpacer(width = 4.dp)
                    subhead1_grey(
                        text = it,
                        modifier = Modifier.alignByBaseline()
                    )
                }
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

        Crossfade(targetState = uiState.viewState) {
            when (it) {
                is ViewState.Error -> {
                    val height = if (uiState.hasVolumes) 268.dp else 224.dp
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(
                                    color = ComposeAppTheme.colors.raina,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.size(48.dp),
                                painter = painterResource(R.drawable.ic_sync_error),
                                contentDescription = null,
                                tint = ComposeAppTheme.colors.grey
                            )
                        }
                        VSpacer(height = 32.dp)
                        subhead2_grey(text = stringResource(id = R.string.SyncError))
                    }
                }
                ViewState.Loading -> Unit
                ViewState.Success -> {
                    Column {
                        HsChartLineHeader(selectedPoint ?: uiState.chartHeaderView)

                        PriceVolChart(
                            chartInfoData = uiState.chartInfoData,
                            loading = uiState.loading,
                            hasVolumes = uiState.hasVolumes,
                            chartViewType = uiState.chartViewType,
                        ) { item ->
                            selectedPoint = item?.let {
                                chartViewModel.getSelectedPoint(it)
                            }
                        }
                    }
                }
            }
        }

        VSpacer(height = 8.dp)
        ChartTab(
            tabItems = uiState.tabItems,
        ) {
            chartViewModel.onSelectChartInterval(it)
            onSelectChartInterval?.invoke(it)
        }
    }
}

@Composable
fun PriceVolChart(
    chartInfoData: ChartInfoData?,
    loading: Boolean,
    hasVolumes: Boolean,
    chartViewType: ChartViewType,
    onSelectPoint: (ChartPoint?) -> Unit,
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

                    override fun onTouchSelect(item: ChartPoint) {
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

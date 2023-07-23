package cash.p.terminal.modules.coin.overview.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.painterResource
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
import cash.p.terminal.ui.compose.components.HSpacer
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.TabButtonSecondaryTransparent
import cash.p.terminal.ui.compose.components.TabItem
import cash.p.terminal.ui.compose.components.TabPeriod
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.diffColor
import cash.p.terminal.ui.compose.components.formatValueAsDiff
import cash.p.terminal.ui.compose.components.subhead1_grey
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.compose.components.subhead2_jacob
import io.horizontalsystems.chartview.Chart
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.chartview.CurveAnimator2
import io.horizontalsystems.chartview.models.ChartIndicator
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.HsTimePeriod
import kotlinx.coroutines.launch

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
                            selectedPoint = item?.let { chartPoint ->
                                chartViewModel.getSelectedPoint(chartPoint)
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

    Box(
        modifier = Modifier
            .height(120.dp)
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        chartInfoData?.let {
            val chartData = it.chartData
            var minValue = chartData.minValue
            val maxValue = chartData.maxValue
            if (minValue == maxValue) {
                minValue *= 0.9f
            }
            var minKey = chartData.startTimestamp
            var maxKey = chartData.endTimestamp
            if (minKey == maxKey) {
                minKey = (minKey * 0.9).toLong()
                maxKey = (maxKey * 1.1).toLong()
            }

            val mainCurve = remember {
                CurveAnimator2(
                    chartData.valuesByTimestamp(),
                    minKey,
                    maxKey,
                    minValue,
                    maxValue
                )
            }

            val curves = remember(chartData.indicators.keys) {
                val curves = mutableMapOf<String, CurveAnimator2>()
                chartData.indicators.forEach { (id, u: ChartIndicator) ->
                    if (u is ChartIndicator.MovingAverage) {
                        curves[id] = CurveAnimator2(
                            u.line,
                            minKey,
                            maxKey,
                            minValue,
                            maxValue
                        ).apply {
                            color = u.color
                        }
                    }
                }
                curves
            }

            chartData.indicators.forEach { (id, u: ChartIndicator) ->
                if (u is ChartIndicator.MovingAverage) {
                    curves[id]?.setTo(
                        u.line,
                        minKey,
                        maxKey,
                        minValue,
                        maxValue,
                    )
                }
            }

            mainCurve.setTo(
                chartData.valuesByTimestamp(),
                minKey,
                maxKey,
                minValue,
                maxValue
            )

            val mainCurveState = mainCurve.state

            val curveStates = curves.map { (t, u) ->
                t to u.state
            }.toMap()

            val scope = rememberCoroutineScope()
            DisposableEffect(chartData) {
                val animationJob = scope.launch {
                    animate(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = tween(1000, easing = LinearEasing),
                    ) { value, _ ->
                        mainCurve.nextFrame(value)
                        curves.forEach { (t, u) ->
                            u.nextFrame(value)
                        }
                    }
                }

                onDispose {
                    animationJob.cancel()
                }
            }

            ChartLineWithGradient(
                mainCurveState.values,
                mainCurveState.startTimestamp,
                mainCurveState.endTimestamp,
                mainCurveState.minValue,
                mainCurveState.maxValue
            )

            curveStates.forEach { (id, it) ->
                val color = try {
                    Color(android.graphics.Color.parseColor(it.color))
                } catch (e: Exception) {
                    Color.Gray
                }

                ChartLine(
                    Modifier.fillMaxSize(),
                    it.values,
                    it.startTimestamp,
                    it.endTimestamp,
                    it.minValue,
                    it.maxValue,
                    color
                )
            }
        }
    }


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
fun ChartLineWithGradient(
    valuesByTimestamp: LinkedHashMap<Long, Float>,
    minKey: Long,
    maxKey: Long,
    minValue: Float,
    maxValue: Float
) {
    Canvas(
        modifier = Modifier
            .height(120.dp)
            .fillMaxWidth(),
        onDraw = {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val xRatio = canvasWidth / (maxKey - minKey)
            val yRatio = canvasHeight / (maxValue - minValue)

            val linePath = Path()
            var pathStarted = false
            valuesByTimestamp.forEach { (timestamp, value) ->
                val x = (timestamp - minKey) * xRatio
                val y = (value - minValue) * yRatio

                if (!pathStarted) {
                    linePath.moveTo(x, y)
                    pathStarted = true
                } else {
                    linePath.lineTo(x, y)
                }
            }

            val gradientPath = Path()
            gradientPath.addPath(linePath)

            gradientPath.lineTo(canvasWidth, 0f)
            gradientPath.lineTo(0f, 0f)
            gradientPath.close()

            scale(scaleX = 1f, scaleY = -1f) {
                drawPath(
                    linePath,
                    Color(0xFF05C46B),
                    style = Stroke(1.dp.toPx())
                )
                drawPath(
                    gradientPath,
                    Brush.verticalGradient(
                        0.00f to Color(0x00416BFF),
                        1.00f to Color(0x8013D670)
                    ),
                )
            }
        }
    )
}

@Composable
fun ChartLine(
    modifier: Modifier,
    data: LinkedHashMap<Long, Float>,
    minKey: Long,
    maxKey: Long,
    minValue: Float,
    maxValue: Float,
    color: Color
) {
    Canvas(
        modifier = modifier,
        onDraw = {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val xRatio = canvasWidth / (maxKey - minKey)
            val yRatio = canvasHeight / (maxValue - minValue)

            val linePath = Path()
            var pathStarted = false
            data.forEach { (key, value) ->
                val x = (key - minKey) * xRatio
                val y = (value - minValue) * yRatio

                if (!pathStarted) {
                    linePath.moveTo(x, y)
                    pathStarted = true
                } else {
                    linePath.lineTo(x, y)
                }
            }

            scale(scaleX = 1f, scaleY = -1f) {
                drawPath(
                    linePath,
                    color,
                    style = Stroke(1.dp.toPx())
                )
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

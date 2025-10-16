package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.chart.ChartModule
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.GraphicBars
import io.horizontalsystems.bankwallet.ui.compose.components.GraphicBarsWithNegative
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsPeriodsScrollableTabRow
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.TabButtonSecondaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.TabPeriod
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.diffColor
import io.horizontalsystems.bankwallet.ui.compose.components.formatValueAsDiff
import io.horizontalsystems.bankwallet.ui.compose.components.micro_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_jacob
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabItem
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.chartview.models.ChartVolumeType
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.HsTimePeriod
import kotlinx.coroutines.launch

private const val MAX_WIDTH = 600

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
            .height(72.dp)
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
                        style = ComposeAppTheme.typography.subhead,
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
                            text = stringResource(extraData.type.titleResId),
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
                                    style = ComposeAppTheme.typography.subheadR,
                                    color = diffColor(diff.raw())
                                )
                            }
                        }
                    }
                }

                is ChartModule.ChartHeaderExtraData.Indicators -> {
                    val styleSubhead = ComposeAppTheme.typography.subheadR
                    Column(
                        modifier = Modifier.width(IntrinsicSize.Max),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row {
                            var textStyle by remember { mutableStateOf(styleSubhead) }
                            var readyToDraw by remember { mutableStateOf(false) }
                            var textSize by remember { mutableStateOf<IntSize?>(null) }
                            Text(
                                text = buildAnnotatedString {
                                    extraData.movingAverages.forEachIndexed { index, item ->
                                        withStyle(style = SpanStyle(color = Color(item.color))) {
                                            append(item.value.plainString())
                                            if (index < extraData.movingAverages.size - 1) {
                                                append(" ")
                                            }
                                        }
                                    }
                                },
                                style = textStyle,
                                modifier = Modifier.drawWithContent {
                                    if (readyToDraw) drawContent()
                                },
                                onTextLayout = { textLayoutResult ->
                                    textSize = textLayoutResult.size
                                    textSize?.let { size ->
                                        if (size.width > MAX_WIDTH) {
                                            textStyle =
                                                textStyle.copy(fontSize = textStyle.fontSize * 0.9)
                                        } else {
                                            readyToDraw = true
                                        }
                                    }
                                }
                            )
                        }

                        if (extraData.rsi != null) {
                            subhead2_jacob(
                                text = App.numberFormatter.formatFiatShort(
                                    extraData.rsi.toBigDecimal(),
                                    "",
                                    8
                                )
                            )
                        } else if (extraData.macd != null) {
                            val macd = extraData.macd
                            Row {
                                var textStyle by remember { mutableStateOf(styleSubhead) }
                                var readyToDraw by remember { mutableStateOf(false) }
                                var textSize by remember { mutableStateOf<IntSize?>(null) }
                                Text(
                                    text = buildAnnotatedString {
                                        macd.histogramValue?.let { value ->
                                            val color =
                                                if (value >= 0) ComposeAppTheme.colors.remus else ComposeAppTheme.colors.lucian
                                            withStyle(style = SpanStyle(color = color)) {
                                                append(value.plainString())
                                                append(" ")
                                            }
                                        }
                                        macd.signalValue?.let { value ->
                                            withStyle(style = SpanStyle(color = ComposeAppTheme.colors.issykBlue)) {
                                                append(value.plainString())
                                                append(" ")
                                            }
                                        }
                                        withStyle(style = SpanStyle(color = ComposeAppTheme.colors.jacob)) {
                                            append(macd.macdValue.plainString())
                                        }
                                    },
                                    style = textStyle,
                                    modifier = Modifier.drawWithContent {
                                        if (readyToDraw) drawContent()
                                    },
                                    onTextLayout = { textLayoutResult ->
                                        textSize = textLayoutResult.size
                                        textSize?.let { size ->
                                            if (size.width > MAX_WIDTH) {
                                                textStyle =
                                                    textStyle.copy(fontSize = textStyle.fontSize * 0.9)
                                            } else {
                                                readyToDraw = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Float.plainString(): String {
    return App.numberFormatter.format(this, 0, 8)
}

@Composable
fun Chart(
    chartViewModel: ChartViewModel,
    modifier: Modifier = Modifier,
    onSelectChartInterval: ((HsTimePeriod?) -> Unit)? = null
) {
    val uiState = chartViewModel.uiState

    Column(
        modifier = modifier
    ) {
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

                        val loadingModifier =
                            if (uiState.loading) Modifier.alpha(0.5f) else Modifier
                        Box(
                            modifier = loadingModifier.fillMaxWidth()
                        ) {
                            PriceVolChart(
                                chartInfoData = uiState.chartInfoData,
                                hasVolumes = uiState.hasVolumes,
                                chartViewType = uiState.chartViewType,
                            ) { item ->
                                selectedPoint = item?.let { selectedItem ->
                                    chartViewModel.getSelectedPoint(selectedItem)
                                }
                            }

                            if (uiState.loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.Center),
                                    color = ComposeAppTheme.colors.grey,
                                    strokeWidth = 2.dp
                                )
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
    hasVolumes: Boolean,
    chartViewType: ChartViewType,
    onSelectPoint: (SelectedItem?) -> Unit,
) {
    val height = if (hasVolumes) 204.dp else 160.dp

    if (chartInfoData == null) {
        Box(modifier = Modifier.height(height))
        return
    }

    val chartData = chartInfoData.chartData

    val colors = ComposeAppTheme.colors

    val chartHelper = remember { ChartHelper(chartData, hasVolumes, colors) }
    chartHelper.setTarget(chartData, hasVolumes)

    val scope = rememberCoroutineScope()
    DisposableEffect(chartData) {
        val animationJob = scope.launch {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(1000, easing = LinearEasing),
            ) { value, _ ->
                chartHelper.onNextFrame(value)
            }
        }

        onDispose {
            animationJob.cancel()
        }
    }

    val mainCurveState = chartHelper.getMainCurveState()
    val dominanceCurveState = chartHelper.getDominanceCurveState()
    val movingAverageCurveStates = chartHelper.getMovingAverageCurveStates()
    val volumeBarsState = chartHelper.getVolumeBarsState()
    val rsiCurveState = chartHelper.getRsiCurveState()
    val macdLineCurveState = chartHelper.getMacdLineCurveState()
    val macdSignalCurveState = chartHelper.getMacdSignalCurveState()
    val macdHistogramBarsState = chartHelper.getMacdHistogramBarsState()
    val selectedItem = chartHelper.selectedItem
    val context = LocalContext.current
    LaunchedEffect(selectedItem) {
        if (selectedItem != null) {
            HudHelper.vibrate(context)
        }

        onSelectPoint.invoke(selectedItem)
    }

    Column {
        Row(
            modifier = Modifier
                .height(20.dp)
                .fillMaxWidth()
                .drawBehind {
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    drawLine(
                        color = colors.blade,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        pathEffect = pathEffect
                    )
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            micro_grey(
                text = chartInfoData.maxValue ?: "",
            )
        }

        Box {
            Column {
                Box(
                    modifier = Modifier
                        .height(120.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    when (chartViewType) {
                        ChartViewType.Line -> {
                            val curveColor: Color
                            val gradientColors: Pair<Color, Color>
                            if (selectedItem == null) {
                                curveColor = chartHelper.mainCurveColor
                                gradientColors = chartHelper.mainCurveGradientColors
                            } else {
                                curveColor = chartHelper.mainCurvePressedColor
                                gradientColors = chartHelper.mainCurveGradientPressedColors
                            }
                            GraphicLineWithGradient(
                                mainCurveState.values,
                                mainCurveState.startTimestamp,
                                mainCurveState.endTimestamp,
                                mainCurveState.minValue,
                                mainCurveState.maxValue,
                                curveColor,
                                gradientColors,
                                selectedItem?.timestamp,
                            )
                        }

                        ChartViewType.Bar -> {
                            val color = if (selectedItem == null) {
                                chartHelper.mainBarsColor
                            } else {
                                chartHelper.mainBarsPressedColor
                            }
                            GraphicBars(
                                modifier = Modifier.fillMaxSize(),
                                data = mainCurveState.values,
                                minKey = mainCurveState.startTimestamp,
                                maxKey = mainCurveState.endTimestamp,
                                minValue = mainCurveState.minValue,
                                maxValue = mainCurveState.maxValue,
                                color = color,
                                selectedItemKey = selectedItem?.timestamp
                            )
                        }
                    }

                    dominanceCurveState?.let {
                        GraphicLine(
                            Modifier.fillMaxSize(),
                            dominanceCurveState.values,
                            dominanceCurveState.startTimestamp,
                            dominanceCurveState.endTimestamp,
                            dominanceCurveState.minValue,
                            dominanceCurveState.maxValue,
                            ComposeAppTheme.colors.yellow50
                        )
                    }

                    movingAverageCurveStates.forEach { maCurveState ->
                        val color = maCurveState.color?.let { Color(it) } ?: Color.Gray
                        GraphicLine(
                            Modifier.fillMaxSize(),
                            maCurveState.values,
                            maCurveState.startTimestamp,
                            maCurveState.endTimestamp,
                            maCurveState.minValue,
                            maCurveState.maxValue,
                            color
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .height(20.dp)
                        .fillMaxWidth()
                        .drawBehind {
                            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            drawLine(
                                color = colors.blade,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                pathEffect = pathEffect
                            )
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    micro_grey(
                        text = chartInfoData.minValue ?: "",
                    )
                }

                if (chartHelper.hasVolumes) {
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .fillMaxWidth()
                    ) {
                        if (rsiCurveState != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(0.3f)
                                    .fillMaxWidth()
                                    .drawBehind {
                                        val pathEffect =
                                            PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                        drawLine(
                                            color = colors.blade,
                                            start = Offset(0f, size.height),
                                            end = Offset(size.width, size.height),
                                            pathEffect = pathEffect
                                        )
                                    }
                                    .padding(horizontal = 16.dp),
                            ) {
                                micro_grey(
                                    modifier = Modifier.align(Alignment.CenterStart),
                                    text = "70",
                                )
                            }

                            GraphicLine(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                data = rsiCurveState.values,
                                minKey = rsiCurveState.startTimestamp,
                                maxKey = rsiCurveState.endTimestamp,
                                minValue = 0f,
                                maxValue = 100f,
                                color = ComposeAppTheme.colors.yellow50
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxHeight(0.3f)
                                    .fillMaxWidth()
                                    .drawBehind {
                                        val pathEffect =
                                            PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                        drawLine(
                                            color = colors.blade,
                                            start = Offset(0f, 0f),
                                            end = Offset(size.width, 0f),
                                            pathEffect = pathEffect
                                        )
                                    }
                                    .padding(horizontal = 16.dp),
                            ) {
                                micro_grey(
                                    modifier = Modifier.align(Alignment.CenterStart),
                                    text = "30",
                                )
                            }
                        } else if (macdLineCurveState != null && macdSignalCurveState != null && macdHistogramBarsState != null) {
                            GraphicBarsWithNegative(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                data = macdHistogramBarsState.values,
                                minKey = macdHistogramBarsState.startTimestamp,
                                maxKey = macdHistogramBarsState.endTimestamp,
                                minValue = macdHistogramBarsState.minValue,
                                maxValue = macdHistogramBarsState.maxValue,
                                color = ComposeAppTheme.colors.green50,
                                colorNegative = ComposeAppTheme.colors.red50
                            )
                            GraphicLine(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                data = macdLineCurveState.values,
                                minKey = macdLineCurveState.startTimestamp,
                                maxKey = macdLineCurveState.endTimestamp,
                                minValue = macdLineCurveState.minValue,
                                maxValue = macdLineCurveState.maxValue,
                                color = ComposeAppTheme.colors.yellow50
                            )
                            GraphicLine(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                data = macdSignalCurveState.values,
                                minKey = macdSignalCurveState.startTimestamp,
                                maxKey = macdSignalCurveState.endTimestamp,
                                minValue = macdSignalCurveState.minValue,
                                maxValue = macdSignalCurveState.maxValue,
                                color = ComposeAppTheme.colors.issykBlue
                            )
                        } else if (volumeBarsState != null) {
                            GraphicBars(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                data = volumeBarsState.values,
                                minKey = volumeBarsState.startTimestamp,
                                maxKey = volumeBarsState.endTimestamp,
                                minValue = volumeBarsState.minValue,
                                maxValue = volumeBarsState.maxValue,
                                color = ComposeAppTheme.colors.blade,
                                selectedItemKey = null
                            )
                        }
                    }
                }
            }

            var selectedX by remember {
                mutableStateOf<Float?>(null)
            }

            val dotColor = ComposeAppTheme.colors.leah

            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 8.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { offset ->
                                selectedX = offset.x
                            },
                            onTap = {
                                selectedX = null
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change: PointerInputChange, _: Offset ->
                                selectedX = change.position.x
                            },
                            onDragEnd = {
                                selectedX = null
                            }
                        )
                    },
                onDraw = {
                    val canvasWidth = size.width
                    chartHelper.setSelectedPercentagePositionX(selectedX?.div(canvasWidth))

                    selectedItem?.let {
                        val x = it.percentagePositionX * canvasWidth

                        drawLine(dotColor, Offset(x, 0f), Offset(x, size.height))
                    }
                }
            )
        }
    }
}

@Composable
fun <T> ChartTab(modifier: Modifier = Modifier, tabItems: List<TabItem<T>>, onSelect: (T) -> Unit) {
    val tabIndex = tabItems.indexOfFirst { it.selected }

    TabPeriod(modifier = modifier) {
        HsPeriodsScrollableTabRow(
            selectedTabIndex = tabIndex,
            modifier = Modifier,
            backgroundColor = Color.Transparent,
            edgePadding = 12.dp,
            indicator = {},
            divider = {}
        ) {
            tabItems.forEachIndexed { index, tabItem ->
                val selected = tabIndex == index
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
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
}

val ChartVolumeType.titleResId: Int
    get() = when (this) {
        ChartVolumeType.Volume -> R.string.CoinPage_Volume
        ChartVolumeType.Tvl -> R.string.Market_Vaults_ChartTvl
    }
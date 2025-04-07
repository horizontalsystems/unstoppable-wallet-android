package cash.p.terminal.modules.market.etf

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.ui_compose.BaseComposeFragment

import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.entities.ViewState
import io.horizontalsystems.chartview.chart.GraphicLine
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.ui_compose.components.ImageSource
import cash.p.terminal.ui_compose.components.HSSwipeRefresh
import cash.p.terminal.ui.compose.Select
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui.compose.components.AlertGroup
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonSecondaryWithIcon
import cash.p.terminal.ui.compose.components.DescriptionCard
import io.horizontalsystems.chartview.GraphicBarsWithNegative
import cash.p.terminal.ui_compose.components.HFillSpacer
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HeaderSorting
import cash.p.terminal.ui.compose.components.ListErrorView
import cash.p.terminal.ui.compose.components.MarketCoinClear
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.VSpacer
import io.horizontalsystems.chartview.cell.CellUniversalFixedHeight
import cash.p.terminal.ui_compose.components.headline2_leah
import cash.p.terminal.ui_compose.components.micro_grey
import cash.p.terminal.ui_compose.components.subhead1_leah
import cash.p.terminal.ui_compose.components.subhead1_lucian
import cash.p.terminal.ui_compose.components.subhead1_remus
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.title3_leah
import cash.p.terminal.ui.compose.hsRememberLazyListState
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.core.helpers.HudHelper
import cash.p.terminal.wallet.models.EtfPoint
import java.math.BigDecimal
import kotlin.math.abs

class EtfFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val factory = EtfModule.Factory()
        val viewModel by viewModels<EtfViewModel> { factory }
        EtfPage(viewModel, navController)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EtfPage(
    viewModel: EtfViewModel,
    navController: NavController,
) {
    val uiState = viewModel.uiState
    val title = stringResource(id = R.string.MarketEtf_Title)
    val description = stringResource(id = R.string.MarketEtf_Description)
    var openPeriodSelector by rememberSaveable { mutableStateOf(false) }
    var openSortingSelector by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Close),
                    icon = R.drawable.ic_close,
                    onClick = {
                        navController.popBackStack()
                    }
                )
            )
        )

        HSSwipeRefresh(
            refreshing = uiState.isRefreshing,
            onRefresh = {
                viewModel.refresh()
            }
        ) {
            Crossfade(uiState.viewState, label = "") { viewState ->
                when (viewState) {
                    ViewState.Loading -> {
                        Loading()
                    }

                    is ViewState.Error -> {
                        ListErrorView(
                            stringResource(R.string.SyncError),
                            viewModel::onErrorClick
                        )
                    }

                    ViewState.Success -> {
                        val listState = hsRememberLazyListState(
                            2,
                            uiState.sortBy,
                            uiState.timeDuration
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(bottom = 32.dp),
                        ) {
                            item {
                                DescriptionCard(
                                    title,
                                    description,
                                    ImageSource.Remote("https://cdn.blocksdecoded.com/header-images/ETF_bitcoin@3x.png")
                                )
                            }
                            item {
                                ChartEtf(uiState.chartDataLoading, uiState.etfPoints, uiState.currency)
                            }
                            stickyHeader {
                                HeaderSorting(borderBottom = true, borderTop = true) {
                                    HSpacer(width = 16.dp)
                                    ButtonSecondaryWithIcon(
                                        modifier = Modifier.height(28.dp),
                                        onClick = {
                                            openSortingSelector = true
                                        },
                                        title = stringResource(uiState.sortBy.titleResId),
                                        iconRight = painterResource(R.drawable.ic_down_arrow_20),
                                    )
                                    HSpacer(width = 8.dp)
                                    ButtonSecondaryWithIcon(
                                        modifier = Modifier.height(28.dp),
                                        onClick = {
                                            openPeriodSelector = true
                                        },
                                        title = stringResource(uiState.timeDuration.titleResId),
                                        iconRight = painterResource(R.drawable.ic_down_arrow_20),
                                    )
                                    HSpacer(width = 16.dp)
                                }
                            }
                            items(uiState.viewItems) { viewItem ->
                                MarketCoinClear(
                                    title = viewItem.title,
                                    subtitle = viewItem.subtitle,
                                    coinIconUrl = viewItem.iconUrl,
                                    coinIconPlaceholder = R.drawable.ic_platform_placeholder_24,
                                    value = viewItem.value,
                                    marketDataValue = viewItem.subvalue,
                                    label = viewItem.rank,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (openPeriodSelector) {
        AlertGroup(
            title = R.string.CoinPage_Period,
            select = Select(uiState.timeDuration, viewModel.timeDurations),
            onSelect = { selected ->
                viewModel.onSelectTimeDuration(selected)
                openPeriodSelector = false
            },
            onDismiss = {
                openPeriodSelector = false
            }
        )
    }
    if (openSortingSelector) {
        AlertGroup(
            title = R.string.Market_Sort_PopupTitle,
            select = Select(uiState.sortBy, viewModel.sortByOptions),
            onSelect = { selected ->
                viewModel.onSelectSortBy(selected)
                openSortingSelector = false
            },
            onDismiss = {
                openSortingSelector = false
            }
        )
    }
}

@Composable
fun ChartEtf(loading: Boolean, etfPoints: List<EtfPoint>, currency: Currency) {
    val keys = mutableListOf<Long>()
    val dataDailyInflow = linkedMapOf<Long, Float>()
    val dataTotalInflow = linkedMapOf<Long, Float>()

    etfPoints.forEach { point ->
        val timestamp = point.date.time / 1000

        keys.add(timestamp)
        dataDailyInflow[timestamp] = point.dailyInflow.toFloat()
        dataTotalInflow[timestamp] = point.totalInflow.toFloat()
    }

    var selectedKey by remember {
        mutableStateOf<Long?>(null)
    }

    val isSelected by remember {
        derivedStateOf {
            selectedKey != null
        }
    }

    val context = LocalContext.current
    LaunchedEffect(selectedKey) {
        if (isSelected) {
            HudHelper.vibrate(context)
        }
    }

    val etfPoint = if (isSelected) {
        etfPoints.firstOrNull { it.date.time / 1000 == selectedKey }
    } else {
        etfPoints.lastOrNull()
    }

    val totalInflow = etfPoint?.totalInflow
    val dailyInflow = etfPoint?.dailyInflow
    val totalAssets = etfPoint?.totalAssets
    val dateStr = if (isSelected) {
        etfPoint?.date?.let { DateHelper.getFullDate(it) }
    } else {
        null
    }

    val totalInflowStr = totalInflow?.let {
        App.numberFormatter.formatFiatShort(it, currency.symbol, currency.decimal)
    }

    val dailyInflowStr = dailyInflow?.let {
        val sign = when {
            it == BigDecimal.ZERO -> ""
            it < BigDecimal.ZERO -> "-"
            else -> "+"
        }
        sign + App.numberFormatter.formatFiatShort(it.abs(), currency.symbol, currency.decimal)
    }
    val dailyInflowPositive = dailyInflow != null && dailyInflow > BigDecimal.ZERO

    val totalAssetsStr = totalAssets?.let {
        App.numberFormatter.formatFiatShort(it, currency.symbol, currency.decimal)
    }

    val labelTop = etfPoints.maxOfOrNull { it.dailyInflow }?.let {
        App.numberFormatter.formatFiatShort(it, currency.symbol, currency.decimal)
    } ?: ""

    val labelBottom = etfPoints.minOfOrNull { it.dailyInflow }?.let {
        val sign = when {
            it < BigDecimal.ZERO -> "-"
            else -> ""
        }

        sign + App.numberFormatter.formatFiatShort(it.abs(), currency.symbol, currency.decimal)
    } ?: ""

    Column {
        ChartHeader(
            mainValue = totalInflowStr,
            mainValueStyleLarge = !isSelected,
            mainSubvalue = dateStr,
            secondaryValue = dailyInflowStr,
            secondaryValuePositive = dailyInflowPositive,
            tertiaryTitle = stringResource(id = R.string.MarketEtf_TotalNetAssets),
            tertiaryValue = totalAssetsStr
        )

        val loadingModifier = if (loading) Modifier.alpha(0.5f) else Modifier
        Box(
            modifier = loadingModifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Column(modifier = Modifier.matchParentSize()) {
                if (dataDailyInflow.isNotEmpty()) {
                    ChartLabelTop(labelTop)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        val color = if (isSelected) {
                            cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.grey50
                        } else {
                            cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.remus
                        }
                        val colorNegative = if (isSelected) {
                            cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.grey50
                        } else {
                            cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.lucian
                        }

                        GraphicBarsWithNegative(
                            modifier = Modifier.matchParentSize(),
                            data = dataDailyInflow,
                            color = color,
                            colorNegative = colorNegative,
                        )
                        GraphicLine(
                            modifier = Modifier.matchParentSize(),
                            data = dataTotalInflow,
                            color = ComposeAppTheme.colors.grey50,
                            selectedItemKey = selectedKey
                        )
                        GraphicPointer(
                            modifier = Modifier.matchParentSize(),
                            keys = keys,
                            onSelect = {
                                selectedKey = it
                            }
                        )
                    }

                    ChartLabelBottom(labelBottom)
                }
            }

            if (loading) {
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

@Composable
private fun GraphicPointer(
    modifier: Modifier = Modifier,
    keys: List<Long>,
    onSelect: (Long?) -> Unit
) {
    var selectedX by remember {
        mutableStateOf<Float?>(null)
    }
    val lineColor = cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.leah
    Canvas(
        modifier = modifier
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
            val pointedXPercentage = selectedX?.div(canvasWidth)

            val minKey = keys.min()
            val maxKey = keys.max()

            val interval = maxKey - minKey

            val nearestKey = pointedXPercentage?.let {
                val pointedKeyCalculatedValue = interval * it + minKey
                keys.minBy {
                    abs(it - pointedKeyCalculatedValue)
                }
            }

            nearestKey?.let {
                val percentage = (nearestKey - minKey) / interval.toFloat()

                val x = percentage * canvasWidth

                drawLine(lineColor, Offset(x, 0f), Offset(x, size.height))
            }

            onSelect.invoke(nearestKey)
        }
    )
}

@Composable
private fun ChartLabelBottom(labelBottom: String) {
    val colors = cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors
    Row(
        modifier = Modifier
            .height(20.dp)
            .fillMaxWidth()
            .drawBehind {
                val pathEffect =
                    PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                drawLine(
                    color = colors.steel10,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    pathEffect = pathEffect
                )
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        micro_grey(text = labelBottom)
    }
}

@Composable
private fun ChartLabelTop(
    labelTop: String,
) {
    val colors = cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors

    Row(
        modifier = Modifier
            .height(20.dp)
            .fillMaxWidth()
            .drawBehind {
                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                drawLine(
                    color = colors.steel10,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    pathEffect = pathEffect
                )
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        micro_grey(text = labelTop)
    }
}

@Composable
private fun ChartHeader(
    mainValue: String?,
    mainValueStyleLarge: Boolean,
    mainSubvalue: String?,
    secondaryValue: String?,
    secondaryValuePositive: Boolean,
    tertiaryTitle: String,
    tertiaryValue: String?,
) {
    CellUniversalFixedHeight(height = 64.dp) {
        Column {
            Row {
                mainValue?.let {
                    if (mainValueStyleLarge) {
                        title3_leah(
                            modifier = Modifier.alignByBaseline(),
                            text = it
                        )
                    } else {
                        headline2_leah(
                            modifier = Modifier.alignByBaseline(),
                            text = it
                        )
                    }
                    HSpacer(width = 4.dp)
                }
                secondaryValue?.let {
                    if (secondaryValuePositive) {
                        subhead1_remus(
                            modifier = Modifier.alignByBaseline(),
                            text = it
                        )
                    } else {
                        subhead1_lucian(
                            modifier = Modifier.alignByBaseline(),
                            text = it
                        )
                    }
                }
            }
            mainSubvalue?.let {
                VSpacer(height = 1.dp)
                subhead2_grey(text = it)
            }
        }
        HFillSpacer(minWidth = 8.dp)
        Column(horizontalAlignment = Alignment.End) {
            subhead2_grey(text = tertiaryTitle)
            tertiaryValue?.let {
                subhead1_leah(text = it)
            }
        }
    }
}

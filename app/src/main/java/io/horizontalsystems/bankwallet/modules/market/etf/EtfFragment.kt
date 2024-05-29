package cash.p.terminal.modules.market.etf

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.entities.Currency
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.overview.ui.GraphicLine
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.modules.market.ImageSource
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.HSSwipeRefresh
import cash.p.terminal.ui.compose.Select
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AlertGroup
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonSecondaryWithIcon
import cash.p.terminal.ui.compose.components.DescriptionCard
import cash.p.terminal.ui.compose.components.GraphicBarsWithNegative
import cash.p.terminal.ui.compose.components.HFillSpacer
import cash.p.terminal.ui.compose.components.HSpacer
import cash.p.terminal.ui.compose.components.HeaderSorting
import cash.p.terminal.ui.compose.components.ListErrorView
import cash.p.terminal.ui.compose.components.MarketCoinClear
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.cell.CellUniversalFixedHeight
import cash.p.terminal.ui.compose.components.micro_grey
import cash.p.terminal.ui.compose.components.subhead1_leah
import cash.p.terminal.ui.compose.components.subhead1_lucian
import cash.p.terminal.ui.compose.components.subhead1_remus
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.compose.components.title3_leah
import io.horizontalsystems.marketkit.models.EtfPoint
import java.math.BigDecimal

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
                        val listState = rememberSaveable(
                            uiState.sortBy,
                            saver = LazyListState.Saver
                        ) {
                            LazyListState()
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(bottom = 32.dp),
                        ) {
                            item {
                                DescriptionCard(
                                    title,
                                    description,
                                    ImageSource.Remote("https://cdn.blocksdecoded.com/category-icons/lending@3x.png")
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
                                    subtitle = viewItem.subtitle,
                                    title = viewItem.title,
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
    val dataDailyInflow = LinkedHashMap(
        etfPoints.map { point ->
            point.date.time / 1000 to point.dailyInflow.toFloat()
        }.toMap()
    )

    val dataTotalInflow = LinkedHashMap(
        etfPoints.map { point ->
            point.date.time / 1000 to point.totalInflow.toFloat()
        }.toMap()
    )

    val lastPoint = etfPoints.lastOrNull()
    val totalInflow = lastPoint?.totalInflow
    val dailyInflow = lastPoint?.dailyInflow
    val totalAssets = lastPoint?.totalAssets

    val totalInflowStr = totalInflow?.let {
        App.numberFormatter.formatFiatShort(it, currency.symbol, currency.decimal)
    }

    val dailyInflowStr = dailyInflow?.let {
        App.numberFormatter.formatFiatShort(it.abs(), currency.symbol, currency.decimal)
    }
    val dailyInflowPositive = dailyInflow != null && dailyInflow > BigDecimal.ZERO

    val totalAssetsStr = totalAssets?.let {
        App.numberFormatter.formatFiatShort(it, currency.symbol, currency.decimal)
    }

    val totalAssetsTitle = stringResource(id = R.string.MarketEtf_TotalNetAssets)

    val labelTop = etfPoints.maxOfOrNull { it.dailyInflow }?.let {
        App.numberFormatter.formatFiatShort(it, currency.symbol, currency.decimal)
    } ?: ""

    val labelBottom = etfPoints.minOfOrNull { it.dailyInflow }?.let {
        val sign = if (it < BigDecimal.ZERO) {
            "-"
        } else {
            ""
        }
        sign + App.numberFormatter.formatFiatShort(it.abs(), currency.symbol, currency.decimal)
    } ?: ""

    Column {
        ChartHeader(totalInflowStr, dailyInflowStr, dailyInflowPositive, totalAssetsTitle, totalAssetsStr)

        val loadingModifier = if (loading) Modifier.alpha(0.5f) else Modifier
        Box(
            modifier = loadingModifier.fillMaxWidth().height(160.dp)
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
                        GraphicBarsWithNegative(
                            modifier = Modifier.matchParentSize(),
                            data = dataDailyInflow,
                            minKey = dataDailyInflow.minOf { it.key },
                            maxKey = dataDailyInflow.maxOf { it.key },
                            minValue = dataDailyInflow.minOf { it.value },
                            maxValue = dataDailyInflow.maxOf { it.value },
                            color = ComposeAppTheme.colors.remus,
                            colorNegative = ComposeAppTheme.colors.lucian,
                        )
                        GraphicLine(
                            modifier = Modifier.matchParentSize(),
                            data = dataTotalInflow,
                            minKey = dataTotalInflow.minOf { it.key },
                            maxKey = dataTotalInflow.maxOf { it.key },
                            minValue = dataTotalInflow.minOf { it.value },
                            maxValue = dataTotalInflow.maxOf { it.value },
                            color = ComposeAppTheme.colors.grey50
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
private fun ChartLabelBottom(labelBottom: String) {
    val colors = ComposeAppTheme.colors
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
    val colors = ComposeAppTheme.colors

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
    secondaryValue: String?,
    secondaryValuePositive: Boolean,
    tertiaryTitle: String,
    tertiaryValue: String?,
) {
    CellUniversalFixedHeight(height = 64.dp) {
        Row {
            mainValue?.let {
                title3_leah(
                    modifier = Modifier.alignByBaseline(),
                    text = it
                )
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
        HFillSpacer(minWidth = 8.dp)
        Column(horizontalAlignment = Alignment.End) {
            subhead2_grey(text = tertiaryTitle)
            tertiaryValue?.let {
                subhead1_leah(text = it)
            }
        }
    }
}

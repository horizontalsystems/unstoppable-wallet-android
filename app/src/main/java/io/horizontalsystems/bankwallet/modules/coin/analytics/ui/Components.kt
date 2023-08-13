package io.horizontalsystems.bankwallet.modules.coin.analytics.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule.BoxItem
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule.OverallScore
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule.ScoreCategory
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.ChartBars
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.StackBarSlice
import io.horizontalsystems.bankwallet.ui.compose.components.StackedBarChart
import io.horizontalsystems.bankwallet.ui.compose.components.TechnicalIndicatorsChart
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_bran
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.chartview.ChartMinimal
import io.horizontalsystems.marketkit.models.HsPointTimePeriod

@Composable
fun AnalyticsBlockHeader(
    title: String,
    onInfoClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        subhead1_grey(text = title)
        onInfoClick?.let {
            HsIconButton(
                modifier = Modifier.size(20.dp),
                onClick = it
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_info_20),
                    contentDescription = "info button",
                    tint = ComposeAppTheme.colors.grey
                )
            }
        }
    }
}

@Composable
fun AnalyticsContentNumber(
    number: String,
    period: String? = null
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        headline1_bran(text = number)
        period?.let {
            HSpacer(8.dp)
            subhead1_grey(
                text = it,
                modifier = Modifier.padding(bottom = 1.dp),
            )
        }
    }
    VSpacer(12.dp)
}

@Composable
fun AnalyticsFooterCell(
    title: BoxItem,
    value: BoxItem?,
    showTopDivider: Boolean = true,
    cellAction: CoinAnalyticsModule.ActionType?,
    onActionClick: (CoinAnalyticsModule.ActionType) -> Unit
) {
    if (showTopDivider) {
        Divider(
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10,
            modifier = Modifier.fillMaxWidth()
        )
    }
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = if (cellAction != null) {
            { onActionClick.invoke(cellAction) }
        } else {
            null
        }
    ) {
        BoxItemCell(
            modifier = Modifier.weight(1f),
            boxItem = title,
            onActionClick = onActionClick
        )
        value?.let {
            BoxItemCell(
                boxItem = it,
                onActionClick = onActionClick
            )
        }

        if (cellAction != null) {
            HSpacer(8.dp)
            Image(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = "arrow icon"
            )
        }
    }
}

@Composable
private fun BoxItemCell(
    modifier: Modifier = Modifier,
    boxItem: BoxItem,
    onActionClick: (CoinAnalyticsModule.ActionType) -> Unit
) {
    when (boxItem) {
        is BoxItem.IconTitle -> {
            Image(
                painter = boxItem.image.painter(),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            HSpacer(16.dp)
            subhead2_grey(
                text = boxItem.text.getString(),
                modifier = modifier.padding(end = 8.dp)
            )
        }

        is BoxItem.OverallScoreValue -> {
            RatingCell(boxItem.score)
        }

        is BoxItem.Title -> {
            subhead2_grey(
                text = boxItem.text.getString(),
                modifier = modifier.padding(end = 8.dp)
            )
        }

        is BoxItem.TitleWithInfo -> {
            subhead2_grey(
                text = boxItem.text.getString(),
            )
            HSpacer(8.dp)
            HsIconButton(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(20.dp),
                onClick = {
                    onActionClick.invoke(boxItem.action)
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_info_20),
                    contentDescription = "info button",
                    tint = ComposeAppTheme.colors.grey,
                )
            }
            Spacer(modifier)
        }

        is BoxItem.Value -> {
            subhead1_leah(text = boxItem.text)
        }

        BoxItem.Dots -> {
            subhead1_leah(text = stringResource(R.string.CoinAnalytics_ThreeDots))
        }
    }
}

@Composable
private fun RatingCell(rating: OverallScore) {
    val color = when (rating) {
        OverallScore.Excellent -> Color(0xFF05C46B)
        OverallScore.Good -> Color(0xFFFFA800)
        OverallScore.Fair -> Color(0xFFFF7A00)
        OverallScore.Poor -> Color(0xFFFF3D00)
    }
    Text(
        text = stringResource(rating.title).uppercase(),
        style = ComposeAppTheme.typography.subhead1,
        color = color,
    )
    HSpacer(8.dp)
    Image(
        painter = painterResource(rating.icon),
        contentDescription = null
    )
}

@Composable
fun AnalyticsContainer(
    showFooterDivider: Boolean = true,
    sectionTitle: @Composable (RowScope.() -> Unit)? = null,
    titleRow: @Composable (() -> Unit)? = null,
    sectionDescription: @Composable (() -> Unit)? = null,
    bottomRows: @Composable ColumnScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    VSpacer(12.dp)
    sectionTitle?.let {
        Divider(
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10,
            modifier = Modifier.fillMaxWidth()
        )
        RowUniversal(content = it)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        titleRow?.invoke()
        content.invoke()
        if (showFooterDivider) {
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Column(
            content = bottomRows
        )
    }
    sectionDescription?.invoke()
}

@Composable
fun AnalyticsChart(
    analyticChart: CoinAnalyticsModule.AnalyticChart,
    navController: NavController,
    onPeriodChange: (HsPointTimePeriod) -> Unit,
) {
    when (analyticChart) {
        is CoinAnalyticsModule.AnalyticChart.StackedBars -> {
            StackedBarChart(analyticChart.data, modifier = Modifier.padding(horizontal = 16.dp))
            VSpacer(12.dp)
        }

        is CoinAnalyticsModule.AnalyticChart.Bars -> {
            ChartBars(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(60.dp),
                chartData = analyticChart.data
            )
            VSpacer(12.dp)
        }

        is CoinAnalyticsModule.AnalyticChart.Line -> {
            AndroidView(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(60.dp),
                factory = {
                    ChartMinimal(it)
                },
                update = { view ->
                    view.doOnLayout {
                        view.setData(analyticChart.data)
                    }
                }
            )
            VSpacer(12.dp)
        }

        is CoinAnalyticsModule.AnalyticChart.TechIndicators -> {
            TechnicalIndicatorsChart(
                rows = analyticChart.data,
                selectedPeriod = analyticChart.selectedPeriod,
                navController = navController,
                onPeriodChange = onPeriodChange
            )
        }
    }
}

@Preview
@Composable
private fun Preview_HoldersBlockLocked() {
    val slices = listOf(
        StackBarSlice(value = 50.34f, color = Color(0xBF808085)),
        StackBarSlice(value = 37.75f, color = Color(0x80808085)),
        StackBarSlice(value = 11.9f, color = Color(0x40808085)),
    )
    ComposeAppTheme {
        AnalyticsContainer(
            titleRow = {
                AnalyticsBlockHeader(
                    title = "Holders",
                    onInfoClick = {}
                )
            },
            bottomRows = {
                AnalyticsFooterCell(
                    title = BoxItem.Title(TranslatableString.PlainString("Blockchain 1")),
                    value = BoxItem.Value(stringResource(R.string.CoinAnalytics_ThreeDots)),
                    cellAction = CoinAnalyticsModule.ActionType.Preview,
                    onActionClick = {}
                )
                AnalyticsFooterCell(
                    title = BoxItem.Title(TranslatableString.PlainString("Blockchain 2")),
                    value = BoxItem.Value(stringResource(R.string.CoinAnalytics_ThreeDots)),
                    cellAction = CoinAnalyticsModule.ActionType.Preview,
                    onActionClick = {}
                )
                AnalyticsFooterCell(
                    title = BoxItem.Title(TranslatableString.PlainString("Blockchain 3")),
                    value = BoxItem.Value(stringResource(R.string.CoinAnalytics_ThreeDots)),
                    cellAction = CoinAnalyticsModule.ActionType.Preview,
                    onActionClick = {}
                )
            }
        ) {
            AnalyticsContentNumber(
                number = "•••",
            )
            VSpacer(12.dp)
            StackedBarChart(slices, modifier = Modifier.padding(horizontal = 16.dp))
            VSpacer(16.dp)
        }
    }
}

@Preview
@Composable
private fun Preview_AnalyticsBarChartDisabled() {
    val navController = rememberNavController()
    ComposeAppTheme {
        AnalyticsContainer(
            titleRow = {
                AnalyticsBlockHeader(
                    title = "Dex Volume",
                    onInfoClick = {}
                )
            },
            bottomRows = {
                AnalyticsFooterCell(
                    title = BoxItem.Title(TranslatableString.PlainString("30-Day Rank")),
                    value = BoxItem.Value("•••"),
                    cellAction = CoinAnalyticsModule.ActionType.Preview,
                    onActionClick = {}
                )
            }
        ) {
            AnalyticsContentNumber(
                number = "•••",
            )
            AnalyticsChart(
                CoinAnalyticsModule.zigzagPlaceholderAnalyticChart(false),
                navController,
                {},
            )
            VSpacer(12.dp)
        }
    }
}

@Preview
@Composable
private fun Preview_AnalyticsLineChartDisabled() {
    val navController = rememberNavController()
    ComposeAppTheme {
        AnalyticsContainer(
            titleRow = {
                AnalyticsBlockHeader(
                    title = "Dex Volume",
                    onInfoClick = {}
                )
            },
            bottomRows = {
                AnalyticsFooterCell(
                    title = BoxItem.Title(TranslatableString.PlainString("30-Day Rank")),
                    value = BoxItem.Value("#19"),
                    cellAction = CoinAnalyticsModule.ActionType.Preview,
                    onActionClick = {}
                )
            }
        ) {
            AnalyticsContentNumber(
                number = "•••",
            )
            AnalyticsChart(
                CoinAnalyticsModule.zigzagPlaceholderAnalyticChart(true),
                navController,
                {},
            )
            VSpacer(12.dp)
        }
    }
}

@Preview
@Composable
private fun Preview_HoldersBlock() {
    val slices = listOf(
        StackBarSlice(value = 60f, color = Color(0xFF6B7196)),
        StackBarSlice(value = 31f, color = Color(0xFFF3BA2F)),
        StackBarSlice(value = 8f, color = Color(0xFF8247E5)),
        StackBarSlice(value = 1f, color = Color(0xFFD74F49))
    )
    ComposeAppTheme {
        AnalyticsContainer(
            titleRow = {
                AnalyticsBlockHeader(
                    title = "Defi Cap",
                    onInfoClick = {}
                )
            },
            bottomRows = {
                AnalyticsFooterCell(
                    title = BoxItem.Title(TranslatableString.PlainString("Chain 1")),
                    value = BoxItem.Value("•••"),
                    cellAction = CoinAnalyticsModule.ActionType.Preview,
                    onActionClick = {}
                )
                AnalyticsFooterCell(
                    title = BoxItem.Title(TranslatableString.PlainString("Chain 2")),
                    value = BoxItem.Value("•••"),
                    cellAction = CoinAnalyticsModule.ActionType.Preview,
                    onActionClick = {}
                )
            }
        ) {
            AnalyticsContentNumber(
                number = "\$2.46B",
                period = "last 30d"
            )
            VSpacer(12.dp)
            StackedBarChart(slices, modifier = Modifier.padding(horizontal = 16.dp))
            VSpacer(16.dp)
        }
    }
}

@Preview
@Composable
private fun Preview_AnalyticsRatingScale() {
    val navController = rememberNavController()
    ComposeAppTheme {
        AnalyticsContainer(
            titleRow = {
                AnalyticsBlockHeader(
                    title = "Dex Volume",
                    onInfoClick = {}
                )
            },
            bottomRows = {
                AnalyticsFooterCell(
                    title = BoxItem.TitleWithInfo(
                        TranslatableString.PlainString("Rating Scale"),
                        CoinAnalyticsModule.ActionType.OpenOverallScoreInfo(ScoreCategory.CexScoreCategory)
                    ),
                    value = BoxItem.OverallScoreValue(OverallScore.Fair),
                    cellAction = null,
                    onActionClick = {}
                )
            }
        ) {
            AnalyticsContentNumber(
                number = "•••",
            )
            AnalyticsChart(
                CoinAnalyticsModule.zigzagPlaceholderAnalyticChart(true),
                navController,
                {},
            )
            VSpacer(12.dp)
        }
    }
}

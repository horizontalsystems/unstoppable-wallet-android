package io.horizontalsystems.bankwallet.modules.coin.analytics.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsModule
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ChartBars
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.StackBarSlice
import io.horizontalsystems.bankwallet.ui.compose.components.StackedBarChart
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_bran
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.chartview.ChartMinimal

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
        subhead2_grey(text = title)
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
}

@Composable
fun AnalyticsFooterCell(
    title: String,
    value: String?,
    showTopDivider: Boolean = true,
    leftIcon: ImageSource? = null,
    onClick: (() -> Unit)? = null
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
        onClick = onClick
    ) {
        leftIcon?.let { icon ->
            Image(
                painter = icon.painter(),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            HSpacer(16.dp)
        }
        subhead2_grey(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        value?.let {
            subhead1_leah(text = it)
        }
        onClick?.let {
            HSpacer(8.dp)
            Image(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = "arrow icon"
            )
        }
    }
}

@Composable
fun AnalyticsContainer(
    showFooterDivider: Boolean = true,
    sectionTitle: @Composable (RowScope.() -> Unit)? = null,
    titleRow: @Composable (() -> Unit)? = null,
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
}

@Composable
fun AnalyticsChart(
    analyticChart: CoinAnalyticsModule.AnalyticChart,
) {
    when (analyticChart) {
        is CoinAnalyticsModule.AnalyticChart.StackedBars -> {
            StackedBarChart(analyticChart.data, modifier = Modifier.padding(horizontal = 16.dp))
        }
        is CoinAnalyticsModule.AnalyticChart.Bars -> {
            ChartBars(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(60.dp),
                chartData = analyticChart.data
            )
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
        }
    }
}

@Composable
fun AnalyticsDataLockedBlockNotActivated(
    onClickActivate: () -> Unit
) {
    AnalyticsContainer {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = ComposeAppTheme.colors.steel10,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(48.dp),
                    painter = painterResource(R.drawable.icon_unlocked_48),
                    contentDescription = "lock icon",
                    tint = ComposeAppTheme.colors.jacob
                )
            }
            VSpacer(32.dp)
            subhead2_grey(
                modifier = Modifier.padding(horizontal = 48.dp),
                text = stringResource(R.string.CoinAnalytics_ActivateSubscription),
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
            VSpacer(32.dp)
            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(id = R.string.Button_Activate),
                onClick = onClickActivate
            )
            VSpacer(32.dp)
        }
    }
}

@Composable
fun AnalyticsDataLockedBlockNoSubscription(
    onClickLearnMore: () -> Unit
) {
    AnalyticsContainer {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = ComposeAppTheme.colors.steel10,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(48.dp),
                    painter = painterResource(R.drawable.icon_lock_48),
                    contentDescription = "lock icon",
                    tint = ComposeAppTheme.colors.jacob
                )
            }
            VSpacer(32.dp)
            subhead2_grey(
                modifier = Modifier.padding(horizontal = 48.dp),
                text = stringResource(R.string.CoinAnalytics_PageLocked),
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
            VSpacer(32.dp)
            ButtonPrimaryDefault(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(id = R.string.Button_LearnMore),
                onClick = onClickLearnMore
            )
            VSpacer(32.dp)
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
    AnalyticsContainer(
        titleRow = {
            AnalyticsBlockHeader(
                title = "Holders",
                onInfoClick = {}
            )
        },
        bottomRows = {
            AnalyticsFooterCell(
                title = "Blockchain 1",
                value = stringResource(R.string.CoinAnalytics_ThreeDots),
                leftIcon = ImageSource.Local(R.drawable.ic_platform_placeholder_32),
                onClick = {}
            )
            AnalyticsFooterCell(
                title = "Blockchain 2",
                value = stringResource(R.string.CoinAnalytics_ThreeDots),
                leftIcon = ImageSource.Local(R.drawable.ic_platform_placeholder_32),
                onClick = {}
            )
            AnalyticsFooterCell(
                title = "Blockchain 3",
                value = stringResource(R.string.CoinAnalytics_ThreeDots),
                leftIcon = ImageSource.Local(R.drawable.ic_platform_placeholder_32),
                onClick = {}
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

@Preview
@Composable
private fun Preview_AnalyticsDataLockedBlock() {
    ComposeAppTheme {
        AnalyticsDataLockedBlockNoSubscription {}
    }
}

@Preview
@Composable
private fun Preview_AnalyticsBarChartDisabled() {
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
                    title = "30-Day Rank",
                    value = "#19",
                    onClick = {}
                )
            }
        ) {
            AnalyticsContentNumber(
                number = "•••",
            )
            AnalyticsChart(
                CoinAnalyticsModule.zigzagPlaceholderAnalyticChart(false),
            )
            VSpacer(12.dp)
        }
    }
}

@Preview
@Composable
private fun Preview_AnalyticsLineChartDisabled() {
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
                    title = "30-Day Rank",
                    value = "#19",
                    onClick = {}
                )
            }
        ) {
            AnalyticsContentNumber(
                number = "•••",
            )
            AnalyticsChart(
                CoinAnalyticsModule.zigzagPlaceholderAnalyticChart(true),
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
    AnalyticsContainer(
        titleRow = {
            AnalyticsBlockHeader(
                title = "Defi Cap",
                onInfoClick = {}
            )
        },
        bottomRows = {
            AnalyticsFooterCell(
                title = "30-Day Rank",
                value = "#19",
                leftIcon = ImageSource.Remote("https://cdn.blocksdecoded.com/blockchain-icons/32px/ethereum@3x.png"),
                onClick = {}
            )
            AnalyticsFooterCell(
                title = "Tether",
                value = "0.29%",
                leftIcon = ImageSource.Remote("https://cdn.blocksdecoded.com/blockchain-icons/32px/solana@3x.png"),
                onClick = {}
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

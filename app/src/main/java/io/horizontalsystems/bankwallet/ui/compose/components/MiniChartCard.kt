package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.chartview.ChartMinimal

@Composable
fun MiniChartCard(
    title: String,
    chartViewItem: CoinDetailsModule.ChartViewItem,
    paddingValues: PaddingValues? = null,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(paddingValues = paddingValues ?: PaddingValues(horizontal = 16.dp))
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(ComposeAppTheme.colors.lawrence)
            .height(105.dp)
            .padding(12.dp),
    ) {
        caption_grey(text = title)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = chartViewItem.headerView.value,
            style = ComposeAppTheme.typography.headline1,
            color = ComposeAppTheme.colors.bran,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.Bottom) {
            (chartViewItem.headerView as? CoinDetailsModule.ChartHeaderView.Latest)?.let { latest ->
                Text(
                    text = App.numberFormatter.formatValueAsDiff(latest.diff),
                    style = ComposeAppTheme.typography.subhead1,
                    color = diffColor(latest.diff.percent),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            AndroidView(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 3.dp, bottom = 6.dp)
                    .height(24.dp),
                factory = {
                    ChartMinimal(it)
                },
                update = { view ->
                    view.doOnLayout {
                        view.setData(chartViewItem.chartData)
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

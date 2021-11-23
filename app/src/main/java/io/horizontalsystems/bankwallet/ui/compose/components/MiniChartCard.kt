package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.chartview.ChartMinimal

@Composable
fun MiniChartCard(
    title: String,
    chartViewItem: CoinDetailsModule.ChartViewItem,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = ComposeAppTheme.typography.caption,
                color = ComposeAppTheme.colors.grey
            )
            chartViewItem.badge?.let {
                Badge(text = it)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = chartViewItem.value,
                style = ComposeAppTheme.typography.subhead1,
                color = ComposeAppTheme.colors.bran
            )
            Text(
                text = formatValueAsDiff(chartViewItem.diff),
                style = ComposeAppTheme.typography.subhead1,
                color = diffColor(chartViewItem.diff.raw())
            )
        }
        Row {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                factory = { context ->
                    ChartMinimal(context)
                },
                update = { view ->
                    view.post {
                        view.setData(chartViewItem.chartData)
                    }
                }
            )
        }

    }
}

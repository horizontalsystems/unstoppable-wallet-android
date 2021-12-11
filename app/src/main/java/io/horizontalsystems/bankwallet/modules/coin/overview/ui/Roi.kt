package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.coin.RoiViewItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import io.horizontalsystems.marketkit.models.TimePeriod
import java.math.BigDecimal

@Composable
fun Roi(roi: List<RoiViewItem>) {
    CellSingleLineLawrenceSection(roi) { item ->
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (item) {
                is RoiViewItem.HeaderRowViewItem -> {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = item.title,
                        style = ComposeAppTheme.typography.subhead1,
                        color = ComposeAppTheme.colors.oz,
                        textAlign = TextAlign.Center
                    )
                    item.periods.forEach { period: TimePeriod ->
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(ComposeAppTheme.colors.steel10)
                        )
                        Text(
                            modifier = Modifier.weight(1f),
                            text = period.periodNameStringResId?.let { stringResource(id = it) }
                                ?: "",
                            style = ComposeAppTheme.typography.caption,
                            color = ComposeAppTheme.colors.bran,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is RoiViewItem.RowViewItem -> {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = item.title,
                        style = ComposeAppTheme.typography.caption,
                        color = ComposeAppTheme.colors.grey,
                        textAlign = TextAlign.Center
                    )
                    item.values.forEach { value ->
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(ComposeAppTheme.colors.steel10)
                        )
                        if (value != null) {
                            val sign = if (value >= BigDecimal.ZERO) "+" else "-"
                            val text = App.numberFormatter.format(value.abs(), 0, 2, sign, "%")
                            val color =
                                if (value >= BigDecimal.ZERO) ComposeAppTheme.colors.remus else ComposeAppTheme.colors.lucian

                            Text(
                                modifier = Modifier.weight(1f),
                                text = text,
                                style = ComposeAppTheme.typography.caption,
                                color = color,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(text = "")
                        }
                    }
                }
            }
        }
    }
}

val TimePeriod.periodNameStringResId: Int?
    get() = when (this) {
        TimePeriod.Day7 -> R.string.CoinPage_Performance_Week
        TimePeriod.Day30 -> R.string.CoinPage_Performance_Month
        else -> null
    }

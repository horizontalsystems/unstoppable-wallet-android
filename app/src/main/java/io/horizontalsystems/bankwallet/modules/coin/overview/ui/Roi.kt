package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.coin.RoiViewItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.marketkit.models.HsTimePeriod
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
                    subhead1_leah(
                        modifier = Modifier.weight(1f),
                        text = item.title,
                        textAlign = TextAlign.Center
                    )
                    item.periods.forEach { period: HsTimePeriod ->
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
                    caption_grey(
                        modifier = Modifier.weight(1f),
                        text = item.title,
                        textAlign = TextAlign.Center
                    )
                    item.values.forEach { value ->
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(ComposeAppTheme.colors.steel10)
                        )
                        val text: String
                        val color: Color
                        if (value != null) {
                            val sign = if (value >= BigDecimal.ZERO) "+" else "-"
                            text = App.numberFormatter.format(value.abs(), 0, 2, sign, "%")
                            color = if (value >= BigDecimal.ZERO) ComposeAppTheme.colors.remus else ComposeAppTheme.colors.lucian

                        } else {
                            text = "---"
                            color = ComposeAppTheme.colors.grey
                        }

                        Text(
                            modifier = Modifier.weight(1f),
                            text = text,
                            style = ComposeAppTheme.typography.caption,
                            color = color,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

val HsTimePeriod.periodNameStringResId: Int?
    get() = when (this) {
        HsTimePeriod.Week1 -> R.string.CoinPage_Performance_Week
        HsTimePeriod.Month1 -> R.string.CoinPage_Performance_Month
        else -> null
    }

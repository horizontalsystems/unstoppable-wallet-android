package io.horizontalsystems.bankwallet.ui.compose

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.core.App
import java.math.BigDecimal

@Composable
fun RateColor(diff: BigDecimal?) =
    if (diff ?: BigDecimal.ZERO >= BigDecimal.ZERO) ComposeAppTheme.colors.remus else ComposeAppTheme.colors.lucian

@Composable
fun RateText(diff: BigDecimal?): String {
    if (diff == null) return ""
    val sign = if (diff >= BigDecimal.ZERO) "+" else "-"
    return App.numberFormatter.format(diff.abs(), 0, 2, sign, "%")
}

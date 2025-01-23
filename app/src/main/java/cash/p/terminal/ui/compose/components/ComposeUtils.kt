package cash.p.terminal.ui.compose.components

import androidx.compose.runtime.Composable
import cash.p.terminal.core.App
import io.horizontalsystems.core.entities.Value
import java.math.BigDecimal

@Composable
fun formatValueAsDiff(value: Value): String =
    App.numberFormatter.formatValueAsDiff(value)

@Composable
fun diffText(diff: BigDecimal?): String {
    if (diff == null) return ""
    val sign = when {
        diff == BigDecimal.ZERO -> ""
        diff >= BigDecimal.ZERO -> "+"
        else -> "-"
    }
    return App.numberFormatter.format(diff.abs(), 0, 2, sign, "%")
}

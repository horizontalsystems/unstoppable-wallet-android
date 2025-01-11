package cash.p.terminal.ui_compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import java.math.BigDecimal

@Composable
fun diffColor(value: BigDecimal?) : Color {
    val diff = value ?: BigDecimal.ZERO
    return when {
        diff.signum() == 0 -> ComposeAppTheme.colors.grey
        diff.signum() >= 0 -> ComposeAppTheme.colors.remus
        else -> ComposeAppTheme.colors.lucian
    }
}
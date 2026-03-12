package cash.p.terminal.ui.compose.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun CardsSwapInfo(
    isError: Boolean = false,
    isWarning: Boolean = false,
    content: @Composable (ColumnScope.() -> Unit)
) {
    val borderColor = when {
        isError -> ComposeAppTheme.colors.lucian
        isWarning -> ComposeAppTheme.colors.jacob
        else -> ComposeAppTheme.colors.steel20
    }
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(vertical = 2.dp),
        content = content
    )
}

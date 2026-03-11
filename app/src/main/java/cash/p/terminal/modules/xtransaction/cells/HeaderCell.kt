package cash.p.terminal.modules.xtransaction.cells

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.subhead1_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.ui_compose.components.CellUniversal

@Composable
fun HeaderCell(
    title: String,
    value: String,
    borderTop: Boolean = true,
    painter: Painter?
) {
    CellUniversal(borderTop = borderTop) {
        painter?.let {
            Icon(
                modifier = Modifier.padding(end = 16.dp),
                painter = painter,
                tint = ComposeAppTheme.colors.grey,
                contentDescription = null,
            )
        }

        body_leah(text = title)

        subhead1_grey(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            text = value,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
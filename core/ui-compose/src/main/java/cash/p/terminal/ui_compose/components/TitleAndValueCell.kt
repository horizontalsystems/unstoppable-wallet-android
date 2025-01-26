package cash.p.terminal.ui_compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun TitleAndValueCell(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    RowUniversal(
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        subhead2_grey(text = title, modifier = Modifier.padding(end = 16.dp))
        Spacer(Modifier.weight(1f))
        subhead1_leah(text = value, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TitleAndTwoValuesCell(
    title: String,
    value: String,
    value2: String?,
    minHeight: Dp = 48.dp,
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        minHeight = minHeight
    ) {
        subhead2_grey(text = title, modifier = Modifier.padding(end = 16.dp))
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            subhead1_leah(text = value, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if(value2 != null) {
                subhead2_grey(text = value2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
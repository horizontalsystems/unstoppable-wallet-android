package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun Description(
    text: String,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    subhead2_grey(
        text = text,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = onTextLayout,
    )
}

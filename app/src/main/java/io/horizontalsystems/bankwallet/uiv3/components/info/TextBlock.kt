package io.horizontalsystems.bankwallet.uiv3.components.info

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.components.subhead_grey

@Composable
fun TextBlock(
    text: String,
    textAlign: TextAlign = TextAlign.Start
) {
    subhead_grey(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        text = text,
        textAlign = textAlign
    )
}
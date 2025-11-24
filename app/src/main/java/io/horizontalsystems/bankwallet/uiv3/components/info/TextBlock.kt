package io.horizontalsystems.bankwallet.uiv3.components.info

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.components.subhead_grey

@Composable
fun TextBlock(text: String) {
    subhead_grey(
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
        text = text
    )
}
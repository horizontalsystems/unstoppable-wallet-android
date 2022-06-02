package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InfoText(text: String) {
    subhead2_grey(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        text = text
    )
}

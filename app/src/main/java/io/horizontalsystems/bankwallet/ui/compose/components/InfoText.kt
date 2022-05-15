package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun InfoText(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        text = text,
        color = ComposeAppTheme.colors.grey,
        style = ComposeAppTheme.typography.subhead2
    )
}

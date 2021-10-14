package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun Description(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        style = ComposeAppTheme.typography.subhead2,
        color = ComposeAppTheme.colors.grey,
    )
}

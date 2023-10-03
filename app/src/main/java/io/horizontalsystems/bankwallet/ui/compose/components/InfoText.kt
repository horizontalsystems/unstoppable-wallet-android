package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun InfoText(
    text: String,
    paddingBottom: Dp = 12.dp,
) {
    subhead2_grey(
        modifier = Modifier.padding(start = 32.dp, top = 12.dp, end = 32.dp, bottom = paddingBottom),
        text = text
    )
}

@Composable
fun InfoTextBody(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
        text = text,
        style = ComposeAppTheme.typography.body,
        color = ComposeAppTheme.colors.bran
    )
}

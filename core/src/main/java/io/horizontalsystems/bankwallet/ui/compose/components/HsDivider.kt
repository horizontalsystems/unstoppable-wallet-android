package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun HsDivider(modifier: Modifier = Modifier) {
    Divider(
        thickness = 0.5.dp,
        color = ComposeAppTheme.colors.blade,
        modifier = modifier
    )
}

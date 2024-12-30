package io.horizontalsystems.chartview.cell

import androidx.compose.foundation.layout.Box
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun BoxBorderedTop(
    content: @Composable () -> Unit
) {
    Box {
        Divider(
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        content()
    }
}

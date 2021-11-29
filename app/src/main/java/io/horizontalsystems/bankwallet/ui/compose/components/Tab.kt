package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun TabBalance(
    borderTop: Boolean = false,
    borderBottom: Boolean = false,
    content: @Composable() (RowScope.() -> Unit),
) {
    BarSingleLine(Modifier.padding(horizontal = 16.dp), borderTop, borderBottom, content)
}

@Composable
fun TabPeriod(content: @Composable RowScope.() -> Unit) {
    BarSingleLine(Modifier, true, false, content)
}

@Composable
fun BarSingleLine(
    modifier: Modifier = Modifier,
    borderTop: Boolean = false,
    borderBottom: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        if (borderTop) {
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        if (borderBottom) {
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        Row(
            modifier = modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content.invoke(this)
        }
    }
}

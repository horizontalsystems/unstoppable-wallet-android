package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun BoxTyler44(
    borderTop: Boolean = false,
    borderBottom: Boolean = false,
    content: @Composable () -> Unit
) {
    Box44(
        borderTop = borderTop,
        borderBottom = borderBottom,
        color = ComposeAppTheme.colors.tyler,
        content = content
    )
}

@Composable
fun Box44(
    borderTop: Boolean = false,
    borderBottom: Boolean = false,
    color: Color? = null,
    content: @Composable () -> Unit
) {
    val colorModifier = when {
        color != null -> Modifier.background(color)
        else -> Modifier
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .then(colorModifier)
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

        content.invoke()
    }
}
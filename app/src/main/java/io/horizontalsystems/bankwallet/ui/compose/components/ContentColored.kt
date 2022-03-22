package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.horizontalsystems.bankwallet.modules.transactionInfo.ColorName
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun ContentColored(
    colorName: ColorName,
    content: @Composable() (RowScope.() -> Unit)
) {
    val color = when (colorName) {
        ColorName.Remus -> ComposeAppTheme.colors.remus
        ColorName.Jacob -> ComposeAppTheme.colors.jacob
        ColorName.Leah -> ComposeAppTheme.colors.leah
        ColorName.Grey -> ComposeAppTheme.colors.grey
    }
    Surface(
        contentColor = color,
        color = Color.Transparent
    ) {
        Row {
            content.invoke(this)
        }
    }

}

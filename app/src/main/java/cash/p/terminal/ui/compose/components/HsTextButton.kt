package cash.p.terminal.ui.compose.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import cash.p.terminal.ui_compose.theme.getRippleConfiguration

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HsTextButton(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    CompositionLocalProvider(LocalRippleConfiguration provides getRippleConfiguration()) {
        TextButton(
            onClick = onClick
        ) {
            content()
        }
    }
}
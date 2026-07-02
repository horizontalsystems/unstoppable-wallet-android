package io.horizontalsystems.core.ui.compose.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import io.horizontalsystems.core.ui.compose.MyRippleConfiguration

@Composable
fun HsTextButton(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    CompositionLocalProvider(LocalRippleConfiguration provides MyRippleConfiguration) {
        TextButton(
            onClick = onClick
        ) {
            content()
        }
    }
}
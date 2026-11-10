package io.horizontalsystems.designkit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.designkit.R
import io.horizontalsystems.designkit.theme.ComposeAppTheme

/** The action revealed behind a cell when it is swiped. */
@Composable
fun HSCellButton(
    modifier: Modifier = Modifier,
    icon: Painter,
    iconTint: Color = ComposeAppTheme.colors.leah,
    backgroundColor: Color = ComposeAppTheme.colors.blade,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (pressed) PressedAlpha else 1f }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .background(backgroundColor)
            .padding(horizontal = 38.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = if (enabled) iconTint else ComposeAppTheme.colors.andy
        )
    }
}

@Preview
@Composable
private fun Preview_HSCellButton() {
    ComponentPreview {
        Row(modifier = Modifier.height(73.dp)) {
            HSCellButton(icon = painterResource(R.drawable.arrow_s_down_24)) {}
            HSCellButton(icon = painterResource(R.drawable.arrow_s_down_24), enabled = false) {}
        }
    }
}

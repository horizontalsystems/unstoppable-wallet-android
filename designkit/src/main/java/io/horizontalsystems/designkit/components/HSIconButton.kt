package io.horizontalsystems.designkit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.horizontalsystems.designkit.R

/**
 * @param iconTint overrides the colour the variant gives the icon, in every
 * state including disabled.
 */
@Composable
fun HSIconButton(
    variant: ButtonVariant = ButtonVariant.Primary,
    style: ButtonStyle = ButtonStyle.Solid,
    size: ButtonSize = ButtonSize.Medium,
    icon: Painter,
    iconTint: Color = Color.Unspecified,
    contentDescription: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val buttonSize: Dp
    val iconSize: Dp

    when (size) {
        ButtonSize.Medium -> {
            buttonSize = 64.dp
            iconSize = 24.dp
        }

        ButtonSize.Small -> {
            buttonSize = 32.dp
            iconSize = 20.dp
        }
    }

    ButtonSurface(
        modifier = modifier.size(buttonSize),
        shape = CircleShape,
        appearance = buttonAppearance(variant, style, size, enabled),
        enabled = enabled,
        onClick = onClick,
    ) {
        Icon(
            modifier = Modifier.size(iconSize),
            painter = icon,
            tint = iconTint.takeOrElse {
                LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
            },
            contentDescription = contentDescription,
        )
    }
}

@Preview
@Composable
private fun Preview_IconButton() {
    ComponentPreview {
        ButtonVariant.entries.forEach { variant ->
            ButtonSize.entries.forEach { size ->
                ButtonStyle.entries.forEach { style ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        HSIconButton(
                            variant = variant,
                            style = style,
                            size = size,
                            icon = painterResource(id = R.drawable.arrow_s_down_24),
                            onClick = {}
                        )
                        HSIconButton(
                            variant = variant,
                            style = style,
                            size = size,
                            icon = painterResource(id = R.drawable.arrow_s_down_24),
                            enabled = false,
                            onClick = {}
                        )
                    }
                }
            }
        }
    }
}

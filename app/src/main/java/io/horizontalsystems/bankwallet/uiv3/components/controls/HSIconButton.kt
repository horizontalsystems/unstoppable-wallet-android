package io.horizontalsystems.bankwallet.uiv3.components.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefaults
import io.horizontalsystems.bankwallet.uiv3.components.HSPreview

@Composable
fun HSIconButton(
    variant: ButtonVariant = ButtonVariant.Primary,
    style: ButtonStyle = ButtonStyle.Solid,
    size: ButtonSize = ButtonSize.Medium,
    icon: Painter,
    contentDescription: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val buttonProps = getButtonProps(size, style, variant)

    Button(
        modifier = Modifier.size(buttonProps.buttonSize),
        shape = RoundedCornerShape(percent = 50),
        onClick = onClick,
        colors = ButtonPrimaryDefaults.textButtonColors(
            backgroundColor = buttonProps.backgroundColor,
            contentColor = buttonProps.contentColor,
            disabledBackgroundColor = buttonProps.disabledBackgroundColor,
            disabledContentColor = buttonProps.disabledContentColor,
        ),
        enabled = enabled,
        contentPadding = PaddingValues(0.dp),
        elevation = null
    ) {
        Icon(
            modifier = Modifier.size(buttonProps.iconSize),
            painter = icon,
            contentDescription = contentDescription,
        )
    }
}

@Composable
private fun getButtonProps(size: ButtonSize, style: ButtonStyle, variant: ButtonVariant): IconButtonProps {
    val buttonSize = when (size) {
        ButtonSize.Medium -> 56.dp
        ButtonSize.Small -> 32.dp
    }
    val iconSize = when (size) {
        ButtonSize.Medium -> 24.dp
        ButtonSize.Small -> 20.dp
    }

    val backgroundColor: Color
    val contentColor: Color
    val disabledBackgroundColor: Color
    val disabledContentColor: Color

    when (variant) {
        ButtonVariant.Primary -> {
            when (style) {
                ButtonStyle.Solid -> {
                    backgroundColor = ComposeAppTheme.colors.jacob
                    contentColor = ComposeAppTheme.colors.lawrence
                    disabledBackgroundColor = ComposeAppTheme.colors.blade
                    disabledContentColor = ComposeAppTheme.colors.andy
                }

                ButtonStyle.Transparent -> {
                    backgroundColor = ComposeAppTheme.colors.transparent
                    contentColor = ComposeAppTheme.colors.jacob
                    disabledBackgroundColor = ComposeAppTheme.colors.transparent
                    disabledContentColor = ComposeAppTheme.colors.grey
                }
            }
        }
        ButtonVariant.Secondary -> {
            when (style) {
                ButtonStyle.Solid -> {
                    when (size) {
                        ButtonSize.Medium -> {
                            backgroundColor = ComposeAppTheme.colors.leah
                            contentColor = ComposeAppTheme.colors.lawrence
                            disabledBackgroundColor = ComposeAppTheme.colors.blade
                            disabledContentColor = ComposeAppTheme.colors.andy
                        }
                        ButtonSize.Small -> {
                            backgroundColor = ComposeAppTheme.colors.blade
                            contentColor = ComposeAppTheme.colors.leah
                            disabledBackgroundColor = ComposeAppTheme.colors.blade
                            disabledContentColor = ComposeAppTheme.colors.andy
                        }
                    }
                }

                ButtonStyle.Transparent -> {
                    backgroundColor = ComposeAppTheme.colors.transparent
                    contentColor = ComposeAppTheme.colors.leah
                    disabledBackgroundColor = ComposeAppTheme.colors.transparent
                    disabledContentColor = ComposeAppTheme.colors.grey
                }
            }
        }
    }

    return IconButtonProps(
        buttonSize = buttonSize,
        iconSize = iconSize,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        disabledBackgroundColor = disabledBackgroundColor,
        disabledContentColor = disabledContentColor
    )
}

data class IconButtonProps(
    val buttonSize: Dp,
    val iconSize: Dp,
    val backgroundColor: Color,
    val contentColor: Color,
    val disabledBackgroundColor: Color,
    val disabledContentColor: Color
)

@Preview
@Composable
fun Preview_IconButton() {
    HSPreview {
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
                            icon = painterResource(id = R.drawable.ic_arrow_down_24),
                            enabled = true,
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

package io.horizontalsystems.bankwallet.uiv3.components.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefaults
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSPreview

@Composable
fun HSButton(
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    style: ButtonStyle = ButtonStyle.Solid,
    size: ButtonSize = ButtonSize.Medium,
    title: String,
    icon: Painter? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val buttonProps = getButtonProps(size, style, variant)

    Button(
        modifier = modifier.height(buttonProps.buttonHeight),
        shape = RoundedCornerShape(28.dp),
        onClick = onClick,
        colors = ButtonPrimaryDefaults.textButtonColors(
            backgroundColor = buttonProps.backgroundColor,
            contentColor = buttonProps.contentColor,
            disabledBackgroundColor = buttonProps.disabledBackgroundColor,
            disabledContentColor = buttonProps.disabledContentColor,
        ),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = buttonProps.horizontalPadding),
        elevation = null
    ) {
        icon?.let {
            Icon(
                modifier = Modifier.size(buttonProps.iconSize),
                painter = it,
                contentDescription = null,
            )
            HSpacer(buttonProps.iconRightPadding)
        }
        Text(
            text = title,
            style = buttonProps.textStyle
        )
    }
}

enum class ButtonStyle {
    Solid, Transparent
}

enum class ButtonSize {
    Medium, Small
}

enum class ButtonVariant {
    Primary, Secondary
}

@Composable
private fun getButtonProps(size: ButtonSize, style: ButtonStyle, variant: ButtonVariant): ButtonProps {
    val buttonHeight: Dp
    val horizontalPadding: Dp
    val textStyle: TextStyle
    val iconSize: Dp
    val iconRightPadding: Dp

    when (size) {
        ButtonSize.Medium -> {
            buttonHeight = 56.dp
            textStyle = ComposeAppTheme.typography.headline2
            horizontalPadding = 40.dp
            iconSize = 24.dp
            iconRightPadding = 8.dp
        }
        ButtonSize.Small -> {
            buttonHeight = 32.dp
            textStyle = ComposeAppTheme.typography.captionSB
            horizontalPadding = 16.dp
            iconSize = 20.dp
            iconRightPadding = 4.dp
        }
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
                    disabledContentColor = ComposeAppTheme.colors.andy
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
                    disabledContentColor = ComposeAppTheme.colors.andy
                }
            }
        }
    }

    return ButtonProps(
        buttonHeight = buttonHeight,
        horizontalPadding = horizontalPadding,
        textStyle = textStyle,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        disabledBackgroundColor = disabledBackgroundColor,
        disabledContentColor = disabledContentColor,
        iconSize = iconSize,
        iconRightPadding = iconRightPadding,
    )
}

data class ButtonProps(
    val buttonHeight: Dp,
    val horizontalPadding: Dp,
    val textStyle: TextStyle,
    val backgroundColor: Color,
    val contentColor: Color,
    val disabledBackgroundColor: Color,
    val disabledContentColor: Color,
    val iconSize: Dp,
    val iconRightPadding: Dp
)

@Preview(widthDp = 560)
@Composable
fun Preview_Button() {
    HSPreview {
        ButtonVariant.entries.forEach { variant ->
            ButtonSize.entries.forEach { size ->
                ButtonStyle.entries.forEach { style ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        HSButton(variant = variant, style = style, size = size, title = "Button", icon = null, enabled = true, onClick = {})
                        HSButton(variant = variant, style = style, size = size, title = "Button", icon = null, enabled = false, onClick = {})
                        HSButton(variant = variant, style = style, size = size, title = "Button", icon = painterResource(R.drawable.ic_info_24), enabled = true, onClick = {})
                    }
                }
            }
        }
    }
}

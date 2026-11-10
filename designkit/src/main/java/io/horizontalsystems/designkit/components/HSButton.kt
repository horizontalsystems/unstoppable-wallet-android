package io.horizontalsystems.designkit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.horizontalsystems.designkit.R
import io.horizontalsystems.designkit.theme.ComposeAppTheme

enum class ButtonStyle {
    Solid, Transparent
}

enum class ButtonSize {
    Medium, Small
}

enum class ButtonVariant {
    Primary, Secondary, Destructive
}

data class ButtonConfig(
    val variant: ButtonVariant,
    val style: ButtonStyle,
    val size: ButtonSize,
    val title: String,
    val onClick: () -> Unit,
)

@Composable
fun HSButton(
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    style: ButtonStyle = ButtonStyle.Solid,
    size: ButtonSize = ButtonSize.Medium,
    title: String,
    icon: Painter? = null,
    loadingIndicator: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val height: Dp
    val horizontalPadding: Dp
    val textStyle: TextStyle
    val iconSize: Dp
    val iconGap: Dp

    when (size) {
        ButtonSize.Medium -> {
            height = 64.dp
            horizontalPadding = 40.dp
            textStyle = ComposeAppTheme.typography.headlineB
            iconSize = 24.dp
            iconGap = 8.dp
        }

        ButtonSize.Small -> {
            height = 32.dp
            horizontalPadding = 16.dp
            textStyle = ComposeAppTheme.typography.captionSB
            iconSize = 20.dp
            iconGap = 4.dp
        }
    }

    ButtonSurface(
        modifier = modifier.height(height),
        shape = RoundedCornerShape(percent = 50),
        appearance = buttonAppearance(variant, style, size, enabled),
        enabled = enabled,
        onClick = onClick,
        startPadding = horizontalPadding,
        minHorizontalPadding = 16.dp,
    ) {
        icon?.let {
            Icon(
                modifier = Modifier.size(iconSize),
                painter = it,
                contentDescription = null,
            )
            Spacer(Modifier.width(iconGap))
        }
        if (loadingIndicator) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = ComposeAppTheme.colors.grey,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(iconGap))
        }
        Text(
            text = title,
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun HSButton(
    modifier: Modifier = Modifier,
    buttonConfig: ButtonConfig,
    icon: Painter? = null,
    enabled: Boolean = true,
) {
    HSButton(
        modifier = modifier,
        variant = buttonConfig.variant,
        style = buttonConfig.style,
        size = buttonConfig.size,
        title = buttonConfig.title,
        icon = icon,
        enabled = enabled,
        onClick = buttonConfig.onClick,
    )
}

@Preview(widthDp = 640)
@Composable
private fun Preview_Button() {
    ComponentPreview {
        ButtonVariant.entries.forEach { variant ->
            ButtonSize.entries.forEach { size ->
                ButtonStyle.entries.forEach { style ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        HSButton(variant = variant, style = style, size = size, title = "Button")
                        HSButton(variant = variant, style = style, size = size, title = "Button", enabled = false)
                        HSButton(
                            variant = variant,
                            style = style,
                            size = size,
                            title = "Button",
                            icon = painterResource(R.drawable.arrow_s_down_24),
                        )
                    }
                }
            }
        }
    }
}

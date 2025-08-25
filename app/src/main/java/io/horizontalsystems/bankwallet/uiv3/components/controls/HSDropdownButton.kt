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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefaults
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSPreview

@Composable
fun HSDropdownButton(
    variant: ButtonVariant = ButtonVariant.Primary,
    style: ButtonStyle = ButtonStyle.Solid,
    title: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val buttonProps = getButtonProps(style, variant)

    Button(
        modifier = Modifier.height(32.dp),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        colors = ButtonPrimaryDefaults.textButtonColors(
            backgroundColor = buttonProps.backgroundColor,
            contentColor = buttonProps.contentColor,
            disabledBackgroundColor = buttonProps.disabledBackgroundColor,
            disabledContentColor = buttonProps.disabledContentColor,
        ),
        enabled = enabled,
        contentPadding = PaddingValues(start = 16.dp, end = 10.dp),
        elevation = null
    ) {
        Text(
            text = title,
            style = ComposeAppTheme.typography.captionSB
        )
        HSpacer(2.dp)
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.arrow_s_down_24),
            contentDescription = null,
        )
    }
}

@Composable
private fun getButtonProps(style: ButtonStyle, variant: ButtonVariant): DropdownButtonProps {
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
                    backgroundColor = ComposeAppTheme.colors.blade
                    contentColor = ComposeAppTheme.colors.leah
                    disabledBackgroundColor = ComposeAppTheme.colors.blade
                    disabledContentColor = ComposeAppTheme.colors.andy
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

    return DropdownButtonProps(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        disabledBackgroundColor = disabledBackgroundColor,
        disabledContentColor = disabledContentColor,
    )
}

data class DropdownButtonProps(
    val backgroundColor: Color,
    val contentColor: Color,
    val disabledBackgroundColor: Color,
    val disabledContentColor: Color
)

@Preview
@Composable
fun Preview_HSDropdownButton() {
    HSPreview {
        ButtonVariant.entries.forEach { variant ->
            ButtonStyle.entries.forEach { style ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    HSDropdownButton(variant = variant, style = style, title = "Button", enabled = true, onClick = {})
                    HSDropdownButton(variant = variant, style = style, title = "Button", enabled = false, onClick = {})
                }
            }
        }
    }
}

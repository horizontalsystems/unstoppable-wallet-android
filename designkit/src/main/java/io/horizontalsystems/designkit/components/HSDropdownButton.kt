package io.horizontalsystems.designkit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.designkit.R
import io.horizontalsystems.designkit.theme.ComposeAppTheme

@Composable
fun HSDropdownButton(
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    style: ButtonStyle = ButtonStyle.Solid,
    title: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ButtonSurface(
        modifier = modifier.height(32.dp),
        shape = RoundedCornerShape(percent = 50),
        appearance = buttonAppearance(variant, style, ButtonSize.Small, enabled),
        enabled = enabled,
        onClick = onClick,
        startPadding = 16.dp,
        endPadding = 10.dp,
    ) {
        Text(
            modifier = Modifier.weight(1f, fill = false),
            text = title,
            style = ComposeAppTheme.typography.captionSB,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(2.dp))
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.arrow_s_down_24),
            contentDescription = null,
        )
    }
}

@Preview
@Composable
private fun Preview_HSDropdownButton() {
    ComponentPreview {
        ButtonVariant.entries.forEach { variant ->
            ButtonStyle.entries.forEach { style ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    HSDropdownButton(variant = variant, style = style, title = "Button", onClick = {})
                    HSDropdownButton(variant = variant, style = style, title = "Button", enabled = false, onClick = {})
                }
            }
        }
    }
}

package io.horizontalsystems.bankwallet.uiv3.components.cell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun CellRightInfoTextIcon(
    text: HSString,
    icon: Painter? = null,
    iconTint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
    onIconClick: (() -> Unit)? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.text,
            style = ComposeAppTheme.typography.subheadSB,
            color = when {
                text.color != null -> text.color
                text.dimmed -> ComposeAppTheme.colors.andy
                else -> ComposeAppTheme.colors.grey
            }
        )
        if (icon != null) {
            val clickModifier = if (onIconClick != null) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, color = ComposeAppTheme.colors.leah),
                    onClick = onIconClick
                )
            } else {
                Modifier
            }
            Icon(
                modifier = clickModifier.size(20.dp),
                painter = icon,
                contentDescription = null,
                tint = iconTint
            )
        }
    }
}

@Preview
@Composable
fun Preview_CellRightInfoTextIcon() {
    ComposeAppTheme {
        Column {
            CellRightInfoTextIcon(
                text = "Text".hs,
                icon = painterResource(R.drawable.info_filled_24),
                iconTint = ComposeAppTheme.colors.grey
            )
            CellRightInfoTextIcon(
                text = "Dimmed".hs(dimmed = true),
                icon = painterResource(R.drawable.info_filled_24),
                iconTint = ComposeAppTheme.colors.grey
            )
        }
    }
}
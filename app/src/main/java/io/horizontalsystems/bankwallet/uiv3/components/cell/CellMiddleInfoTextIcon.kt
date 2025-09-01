package io.horizontalsystems.bankwallet.uiv3.components.cell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
fun CellMiddleInfoTextIcon(
    text: HSString,
    icon: Painter? = null,
    iconTint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.text,
            style = ComposeAppTheme.typography.subhead,
            color = text.color ?: ComposeAppTheme.colors.grey,
        )
        if (icon != null) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = icon,
                contentDescription = null,
                tint = iconTint
            )
        }
    }
}

@Preview
@Composable
fun Prev_CellMiddleInfoTextIcon() {
    ComposeAppTheme {
        CellMiddleInfoTextIcon(
            text = "Text".hs,
            icon = painterResource(R.drawable.info_filled_24),
            iconTint = ComposeAppTheme.colors.grey
        )
    }
}

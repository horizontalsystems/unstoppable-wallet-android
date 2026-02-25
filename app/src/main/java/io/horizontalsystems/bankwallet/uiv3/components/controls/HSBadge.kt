package io.horizontalsystems.bankwallet.uiv3.components.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme


@Composable
fun HSBadge(
    modifier: Modifier = Modifier,
    text: String,
    background: Color = ComposeAppTheme.colors.lucian,
    textColor: Color = ComposeAppTheme.colors.white,
) {
    HSBadgeBase(
        modifier = modifier,
        background = SolidColor(background),
    ) {
        Text(
            text = text,
            color = textColor,
            style = ComposeAppTheme.typography.microSB,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun HSBadgeOutline(
    modifier: Modifier = Modifier,
    text: String,
    color: Color,
) {
    Row(
        modifier = modifier
            .border(1.dp, color, RoundedCornerShape(8.dp))
            .padding(start = 6.dp, end = 6.dp, top = 1.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            color = color,
            style = ComposeAppTheme.typography.microSB,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HSBadgeBase(
    modifier: Modifier = Modifier,
    background: Brush,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content
    )
}

@Preview
@Composable
fun HSBadge_Preview() {
    ComposeAppTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HSBadge(text = "#455")
            HSBadgeOutline(text = "STRONG BUY", color = ComposeAppTheme.colors.remus)
            HSBadgeOutline(text = "BUY", color = ComposeAppTheme.colors.remus)
            HSBadgeOutline(text = "STRONG SELL", color = ComposeAppTheme.colors.lucian)
            HSBadgeOutline(text = "SELL", color = ComposeAppTheme.colors.lucian)
            HSBadgeOutline(text = "NEUTRAL", color = ComposeAppTheme.colors.grey)
            HSBadgeOutline(text = "RISKY", color = ComposeAppTheme.colors.jacob)
        }
    }
}
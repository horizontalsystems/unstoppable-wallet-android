package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import io.horizontalsystems.bankwallet.ui.compose.Dark
import java.math.BigDecimal

@Composable
fun Badge(modifier: Modifier = Modifier, text: String) {
    BadgeText(
        modifier = modifier,
        text = text,
        background = ComposeAppTheme.colors.blade,
        textColor = ComposeAppTheme.colors.leah,
    )
}

@Composable
fun BadgeWithDiff(
    modifier: Modifier = Modifier,
    text: String,
    diff: BigDecimal? = null
) {
    BadgeBase(
        modifier = modifier,
        background = SolidColor(ComposeAppTheme.colors.blade)
    ) {
        Text(
            text = text,
            color = ComposeAppTheme.colors.leah,
            style = ComposeAppTheme.typography.microSB,
            maxLines = 1,
        )
        diff?.let { diffValue ->
            Text(
                modifier = Modifier.padding(start = 4.dp),
                text = "${sign(diffValue)}${diff.abs()}",
                color = diffColor(diffValue),
                style = ComposeAppTheme.typography.microSB,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun BadgeText(
    modifier: Modifier = Modifier,
    text: String,
    background: Color = ComposeAppTheme.colors.lucian,
    textColor: Color = ComposeAppTheme.colors.white,
) {
    BadgeBase(
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
fun BadgeOrangeGradient(
    modifier: Modifier = Modifier,
    text: String,
) {
    val gradientColors = listOf(Color(0xFFFFAA00), Color(0xFFFE4A11))
    BadgeBase(
        modifier = modifier,
        background = Brush.horizontalGradient(gradientColors),
    ) {
        Text(
            text = text,
            color = Dark,
            style = ComposeAppTheme.typography.microSB,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}


@Composable
fun BadgeBase(
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

private fun sign(value: BigDecimal): String {
    return when (value.signum()) {
        1 -> "+"
        -1 -> "-"
        else -> ""
    }
}

@Preview
@Composable
fun BadgePreview() {
    ComposeAppTheme {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Badge(text = "#455")
        }
    }
}

@Preview
@Composable
fun BadgeCirclePreview() {
    ComposeAppTheme {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            BadgeText(
                background = ComposeAppTheme.colors.issykBlue,
                textColor = ComposeAppTheme.colors.tyler,
                text = "1"
            )
        }
    }
}

@Preview
@Composable
fun BadgeCircleSignal_Preview() {
    ComposeAppTheme {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            BadgeText(
                background = ComposeAppTheme.colors.red20,
                textColor = ComposeAppTheme.colors.lucian,
                text = "Sell"
            )
        }
    }
}

@Preview
@Composable
fun BadgeWithDiffPreview() {
    ComposeAppTheme {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            BadgeWithDiff(text = "35", diff = BigDecimal("5"))
        }
    }
}

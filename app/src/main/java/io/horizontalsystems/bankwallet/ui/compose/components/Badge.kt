package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.SteelDark
import java.math.BigDecimal

@Composable
fun Badge(modifier: Modifier = Modifier, text: String) {
    Text(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(ComposeAppTheme.colors.jeremy)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        text = text,
        color = ComposeAppTheme.colors.bran,
        style = ComposeAppTheme.typography.microSB,
    )
}

@Composable
fun BadgeWithDiff(
    modifier: Modifier = Modifier,
    text: String,
    diff: BigDecimal? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(ComposeAppTheme.colors.jeremy)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row {
            Text(
                text = text,
                color = ComposeAppTheme.colors.bran,
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
}

@Composable
fun BadgeRatingD(modifier: Modifier = Modifier, text: String) {
    Text(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(ComposeAppTheme.colors.lightGrey)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        text = text,
        color = SteelDark,
        style = ComposeAppTheme.typography.microSB,
    )
}

@Composable
fun BadgeCount(
    modifier: Modifier = Modifier,
    text: String,
    background: Color = ComposeAppTheme.colors.lucian,
    textColor: Color = ComposeAppTheme.colors.white,
) {
    Text(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .defaultMinSize(minWidth = 14.dp),
        text = text,
        color = textColor,
        style = ComposeAppTheme.typography.captionSB,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun BadgeCircle(modifier: Modifier = Modifier, text: String) {
    Text(
        text,
        modifier = modifier
            .background(ComposeAppTheme.colors.lucian, shape = CircleShape)
            .badgeLayout(),
        color = ComposeAppTheme.colors.white,
        style = ComposeAppTheme.typography.subhead1,
    )
}

@Composable
fun BadgeStepCircle(
    modifier: Modifier = Modifier,
    text: String,
    background: Color,
    textColor: Color
) {
    Text(
        text = text,
        modifier = modifier
            .background(background, shape = CircleShape)
            .badgeLayout(),
        color = textColor,
        style = ComposeAppTheme.typography.captionSB,
    )
}

fun Modifier.badgeLayout() =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)

        val backgroundHeight = (placeable.height * 1.5).toInt()
        val minPadding = backgroundHeight / 2

        val width = maxOf(placeable.width + minPadding, backgroundHeight)
        layout(width, backgroundHeight) {
            placeable.place(
                (width - placeable.width) / 2,
                (backgroundHeight - placeable.height) / 2
            )
        }
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
fun BagdePreview() {
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
fun BagdeCountPreview() {
    ComposeAppTheme {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            BadgeCount(text = "55")
        }
    }
}

@Preview
@Composable
fun BagdeCirclePreview() {
    ComposeAppTheme {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            BadgeCircle(text = "1")
        }
    }
}

@Preview
@Composable
fun BagdeStepCircle_Preview() {
    ComposeAppTheme {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            BadgeStepCircle(text = "2", background = ComposeAppTheme.colors.claude,  textColor = ComposeAppTheme.colors.leah)
        }
    }
}
package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.SteelDark

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
fun BadgeRed(modifier: Modifier = Modifier, text: String) {
    Text(
        text,
        modifier = modifier
            .background(ComposeAppTheme.colors.lucian, shape = CircleShape)
            .badgeLayout(),
        color = ComposeAppTheme.colors.white,
        style = ComposeAppTheme.typography.subhead1,
    )
}

fun Modifier.badgeLayout() =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)

        val backgroundHeight = (placeable.height * 1.5).toInt()
        val minPadding = backgroundHeight / 2

        val width = maxOf(placeable.width + minPadding, backgroundHeight)
        layout(width, backgroundHeight) {
            placeable.place((width - placeable.width) / 2, (backgroundHeight - placeable.height)/2)
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
fun BagdeRedPreview() {
    ComposeAppTheme {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            BadgeRed(text = "123")
        }
    }
}
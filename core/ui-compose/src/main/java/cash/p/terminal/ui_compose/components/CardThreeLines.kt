package cash.p.terminal.ui_compose.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun CardThreeLines(
    title: String,
    subtitle: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .padding(12.dp)
    ) {
        Text(
            style = ComposeAppTheme.typography.caption,
            color = ComposeAppTheme.colors.bran.copy(alpha = 0.6f),
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis

        )
        Text(
            style = ComposeAppTheme.typography.headline1.copy(
                fontWeight = FontWeight.Medium
            ),
            color = ComposeAppTheme.colors.leah,
            text = subtitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            style = ComposeAppTheme.typography.subhead1,
            color = ComposeAppTheme.colors.bran,
            text = description,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF888888,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    showBackground = true,
    backgroundColor = 0,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun CardThreeLinesPreview() {
    ComposeAppTheme {
        Box(Modifier.padding(50.dp)) {
            CardThreeLines(
                title = "Total Balance",
                subtitle = "100000",
                description = "$978.16"
            )
        }
    }
}
package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

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
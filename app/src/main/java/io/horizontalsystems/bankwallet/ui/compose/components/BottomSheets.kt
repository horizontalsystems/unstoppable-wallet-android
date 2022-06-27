package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun ImportantText(
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Divider(
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10,
        )
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            content.invoke()
        }
    }
}

@Preview
@Composable
fun ImportantTextPreview() {
    ComposeAppTheme {
        ImportantText {
            TextImportantWarning(text = "Yahoo")
        }
    }
}

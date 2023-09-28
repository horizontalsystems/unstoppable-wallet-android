package cash.p.terminal.ui.compose.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cash.p.terminal.ui.compose.ComposeAppTheme

@Composable
fun InfoText(text: String, paddingValues: PaddingValues? = null) {
    val padding = paddingValues?.let {
        Modifier.padding(it)
    } ?: Modifier.padding(horizontal = 32.dp, vertical = 12.dp)

    subhead2_grey(
        modifier = padding,
        text = text
    )
}

@Composable
fun InfoTextBody(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
        text = text,
        style = ComposeAppTheme.typography.body,
        color = ComposeAppTheme.colors.bran
    )
}

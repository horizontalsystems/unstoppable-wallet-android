package cash.p.terminal.modules.markdown

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.ui_compose.currentYear
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun ColumnScope.MarkdownFooter() {
    Divider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = ComposeAppTheme.colors.steel10
    )

    Spacer(Modifier.height(12.dp))

    Text(
        text = stringResource(R.string.footer_text, currentYear()),
        style = ComposeAppTheme.typography.caption,
        color = ComposeAppTheme.colors.grey,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .align(Alignment.CenterHorizontally)
    )

    Spacer(Modifier.height(28.dp))
}
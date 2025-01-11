package cash.p.terminal.modules.memo

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.ui.compose.components.FormsInput
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun HSMemoInput(
    maxLength: Int,
    onValueChange: (String) -> Unit
) {
    FormsInput(
        modifier = Modifier.padding(horizontal = 16.dp),
        hint = stringResource(R.string.Send_DialogMemoHint),
        hintColor = ComposeAppTheme.colors.grey50,
        hintStyle = ComposeAppTheme.typography.bodyItalic,
        textColor = ComposeAppTheme.colors.leah,
        textStyle = ComposeAppTheme.typography.bodyItalic,
        pasteEnabled = false,
        singleLine = true,
        maxLength = maxLength,
        onValueChange = onValueChange
    )
}

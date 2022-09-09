package io.horizontalsystems.bankwallet.modules.memo

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput

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
        textColor = ComposeAppTheme.colors.light,
        textStyle = ComposeAppTheme.typography.bodyItalic,
        pasteEnabled = false,
        singleLine = true,
        maxLength = maxLength,
        onValueChange = onValueChange
    )
}

package com.quantum.wallet.bankwallet.modules.memo

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.components.FormsInput

@Composable
fun HSMemoInput(
    maxLength: Int,
    memo: String? = null,
    onValueChange: (String) -> Unit
) {
    FormsInput(
        modifier = Modifier.padding(horizontal = 16.dp),
        hint = stringResource(R.string.Send_DialogMemoHint),
        initial = memo,
        hintColor = ComposeAppTheme.colors.andy,
        hintStyle = ComposeAppTheme.typography.bodyItalic,
        textColor = ComposeAppTheme.colors.leah,
        textStyle = ComposeAppTheme.typography.bodyItalic,
        pasteEnabled = false,
        singleLine = true,
        maxLength = maxLength,
        onValueChange = onValueChange
    )
}

package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun HsCheckbox(
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)?,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.clickable(
            enabled = enabled,
            onClick = { onCheckedChange?.invoke(!checked) }
        )
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_checkbox_frame),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey
        )
        if (checked) {
            Icon(
                painter = painterResource(id = R.drawable.ic_checkbox_check),
                contentDescription = null,
                tint = ComposeAppTheme.colors.jacob
            )
        }
    }
}

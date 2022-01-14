package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun HsCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = CheckboxDefaults.colors(
            checkedColor = ComposeAppTheme.colors.jacob,
            uncheckedColor = ComposeAppTheme.colors.grey,
            checkmarkColor = ComposeAppTheme.colors.lawrence
        )
    )
}

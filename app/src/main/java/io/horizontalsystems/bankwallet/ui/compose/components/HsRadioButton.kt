package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun HsRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
) {
    RadioButton(
        selected = selected,
        onClick = onClick,
        colors = RadioButtonDefaults.colors(
            selectedColor = ComposeAppTheme.colors.jacob,
            unselectedColor = ComposeAppTheme.colors.grey
        )
    )
}

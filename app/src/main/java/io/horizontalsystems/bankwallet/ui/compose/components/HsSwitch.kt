package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun HsSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
){
    Switch(
        modifier = modifier,
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = ComposeAppTheme.colors.white,
            uncheckedThumbColor = ComposeAppTheme.colors.lightGrey,
            checkedTrackColor = ComposeAppTheme.colors.yellowD,
            uncheckedTrackColor = ComposeAppTheme.colors.elenaD,
            checkedTrackAlpha = 1f,
            uncheckedTrackAlpha = 0.2f,
        ),
        enabled = enabled
    )
}

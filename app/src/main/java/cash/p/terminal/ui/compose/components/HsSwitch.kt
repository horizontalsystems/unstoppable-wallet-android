package cash.p.terminal.ui.compose.components

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HsSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
){
    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
        Switch(
            modifier = modifier,
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.white,
                uncheckedThumbColor = cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.lightGrey,
                checkedTrackColor = cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.yellowD,
                uncheckedTrackColor = cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.elenaD,
                checkedTrackAlpha = 1f,
                uncheckedTrackAlpha = 0.2f,
            ),
            enabled = enabled
        )
    }
}

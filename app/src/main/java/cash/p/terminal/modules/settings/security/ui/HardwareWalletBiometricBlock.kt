package cash.p.terminal.modules.settings.security.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.settings.security.SecurityCenterCell
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.HsSwitch
import cash.p.terminal.ui_compose.components.InfoText
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
internal fun HardwareWalletBiometricBlock(
    enabled: Boolean,
    onValueChanged:(Boolean) -> Unit
) {
    CellUniversalLawrenceSection {
        SecurityCenterCell(
            start = {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(R.drawable.icon_touch_id_24),
                    tint = ComposeAppTheme.colors.grey,
                    contentDescription = null,
                )
            },
            center = {
                body_leah(
                    text = stringResource(R.string.app_settings_saved_access_codes),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            end = {
                HsSwitch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        onValueChanged(checked)
                    }
                )
            }
        )
    }

    InfoText(
        text = stringResource(R.string.app_settings_saved_access_codes_footer),
        paddingBottom = 32.dp
    )
}

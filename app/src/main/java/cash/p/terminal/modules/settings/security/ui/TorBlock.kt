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
import cash.p.terminal.modules.settings.security.tor.SecurityTorSettingsViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.HsSwitch
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui.compose.components.body_leah

@Composable
fun TorBlock(
    viewModel: SecurityTorSettingsViewModel,
    showAppRestartAlert: () -> Unit,
) {
    if (viewModel.showRestartAlert) {
        showAppRestartAlert()
        viewModel.restartAppAlertShown()
    }

    CellUniversalLawrenceSection {
        SecurityCenterCell(
            start = {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(R.drawable.ic_tor_connection_24),
                    tint = ComposeAppTheme.colors.grey,
                    contentDescription = null,
                )
            },
            center = {
                body_leah(
                    text = stringResource(R.string.Tor_Title),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            end = {
                HsSwitch(
                    checked = viewModel.torCheckEnabled,
                    onCheckedChange = { checked ->
                        viewModel.setTorEnabledWithChecks(checked)
                    }
                )
            }
        )
    }

    InfoText(
        text = stringResource(R.string.SettingsSecurity_TorConnectionDescription),
        paddingBottom = 32.dp
    )
}

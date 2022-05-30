package io.horizontalsystems.bankwallet.modules.settings.security.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.settings.security.tor.SecurityTorSettingsViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellMultilineLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch

@Composable
fun TorBlock(
    viewModel: SecurityTorSettingsViewModel,
    showAppRestartAlert: () -> Unit,
    showNotificationsNotEnabledAlert: () -> Unit,
) {

    if (viewModel.showRestartAlert) {
        showAppRestartAlert()
        viewModel.restartAppAlertShown()
    }

    if (viewModel.showTorNotificationNotEnabledAlert) {
        showNotificationsNotEnabledAlert()
        viewModel.torNotificationNotEnabledAlertShown()
    }

    val connectionState = viewModel.torConnectionStatus


    CellMultilineLawrenceSection(
        listOf {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (connectionState.showConnectionSpinner) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = ComposeAppTheme.colors.grey,
                        strokeWidth = 2.dp
                    )
                } else {
                    connectionState.icon?.let{ icon ->
                        Icon(
                            painter = painterResource(icon),
                            tint = ComposeAppTheme.colors.jacob,
                            contentDescription = null,
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column{
                    Text(
                        text = stringResource(R.string.Tor_Title),
                        style = ComposeAppTheme.typography.body,
                        color = ComposeAppTheme.colors.leah,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = stringResource(connectionState.value),
                        style = ComposeAppTheme.typography.subhead2,
                        color = ComposeAppTheme.colors.grey,
                    )
                }
                Spacer(Modifier.weight(1f))
                HsSwitch(
                    checked = viewModel.torCheckEnabled,
                    onCheckedChange = { checked ->
                        viewModel.setTorEnabledWithChecks(checked)
                    }
                )
            }
        })

    Text(
        text = stringResource(R.string.SettingsSecurity_TorConnectionDescription),
        style = ComposeAppTheme.typography.subhead2,
        color = ComposeAppTheme.colors.grey,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

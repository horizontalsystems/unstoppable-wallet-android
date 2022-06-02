package io.horizontalsystems.bankwallet.modules.settings.security.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
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
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey

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
                    body_leah(text = stringResource(R.string.Tor_Title))
                    Spacer(Modifier.height(1.dp))
                    subhead2_grey(text = stringResource(connectionState.value))
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

    subhead2_grey(
        text = stringResource(R.string.SettingsSecurity_TorConnectionDescription),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

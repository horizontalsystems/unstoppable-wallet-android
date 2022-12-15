package io.horizontalsystems.bankwallet.modules.settings.security.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.settings.security.tor.SecurityTorSettingsViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*

@Composable
fun TorBlock(
    viewModel: SecurityTorSettingsViewModel,
    showAppRestartAlert: () -> Unit,
) {
    if (viewModel.showRestartAlert) {
        showAppRestartAlert()
        viewModel.restartAppAlertShown()
    }

    val connectionState = viewModel.torConnectionStatus


    CellUniversalLawrenceSection(
        listOf {
            RowUniversal(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalPadding = 0.dp,
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
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(icon),
                            tint = ComposeAppTheme.colors.jacob,
                            contentDescription = null,
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.padding(vertical = 12.dp)){
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

    InfoText(
        text = stringResource(R.string.SettingsSecurity_TorConnectionDescription),
    )
}

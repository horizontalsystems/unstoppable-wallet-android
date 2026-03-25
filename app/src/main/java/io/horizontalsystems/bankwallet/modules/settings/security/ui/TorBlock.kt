package io.horizontalsystems.bankwallet.modules.settings.security.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.settings.privacy.tor.SecurityTorSettingsViewModel
import io.horizontalsystems.bankwallet.modules.settings.security.SecurityCenterCell
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorBlock(
    viewModel: SecurityTorSettingsViewModel,
) {
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (viewModel.showRestartAlert) {
        val warningTitle = if (viewModel.torCheckEnabled) {
            Translator.getString(R.string.Tor_Connection_Enable)
        } else {
            Translator.getString(R.string.Tor_Connection_Disable)
        }

        val actionButton = if (viewModel.torCheckEnabled) {
            Translator.getString(R.string.Button_Enable)
        } else {
            Translator.getString(R.string.Button_Disable)
        }

        BottomSheetContent(
            onDismissRequest = {
                viewModel.restartAppAlertShown()
            },
            sheetState = modalBottomSheetState
        ) {
            ConfirmationDialog(
                icon = R.drawable.ic_tor_connection_24,
                title = Translator.getString(R.string.Tor_Alert_Title),
                warningTitle = warningTitle,
                warningText = Translator.getString(R.string.SettingsSecurity_AppRestartWarning),
                actionButtonTitle = actionButton,
                transparentButtonTitle = Translator.getString(R.string.Alert_Cancel),
                onClose = {
                    viewModel.restartAppAlertShown()
                },
                actionButtonXxx = {
                    viewModel.setTorEnabled()
                },
                transparentButtonXxx = {
                    viewModel.resetSwitch()
                }
            )
        }
    }

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
        },
        onClick = {
            viewModel.setTorEnabledWithChecks(!viewModel.torCheckEnabled)
        }
    )
}

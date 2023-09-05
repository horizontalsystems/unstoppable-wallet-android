package io.horizontalsystems.bankwallet.modules.settings.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.settings.security.passcode.SecurityPasscodeSettingsModule
import io.horizontalsystems.bankwallet.modules.settings.security.passcode.SecurityPasscodeSettingsViewModel
import io.horizontalsystems.bankwallet.modules.settings.security.tor.SecurityTorSettingsModule
import io.horizontalsystems.bankwallet.modules.settings.security.tor.SecurityTorSettingsViewModel
import io.horizontalsystems.bankwallet.modules.settings.security.ui.PasscodeBlock
import io.horizontalsystems.bankwallet.modules.settings.security.ui.TorBlock
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import io.horizontalsystems.core.findNavController
import kotlin.system.exitProcess

class SecuritySettingsFragment : BaseComposeFragment() {

    private val torViewModel by viewModels<SecurityTorSettingsViewModel> {
        SecurityTorSettingsModule.Factory()
    }

    private val passcodeViewModel by viewModels<SecurityPasscodeSettingsViewModel> {
        SecurityPasscodeSettingsModule.Factory()
    }

    @Composable
    override fun GetContent() {
        ComposeAppTheme {
            SecurityCenterScreen(
                passcodeViewModel = passcodeViewModel,
                torViewModel = torViewModel,
                navController = findNavController(),
                showAppRestartAlert = { showAppRestartAlert() },
                restartApp = { restartApp() },
            )
        }
    }

    private fun showAppRestartAlert() {
        val warningTitle = if (torViewModel.torCheckEnabled) {
            getString(R.string.Tor_Connection_Enable)
        } else {
            getString(R.string.Tor_Connection_Disable)
        }

        val actionButton = if (torViewModel.torCheckEnabled) {
            getString(R.string.Button_Enable)
        } else {
            getString(R.string.Button_Disable)
        }

        ConfirmationDialog.show(
            icon = R.drawable.ic_tor_connection_24,
            title = getString(R.string.Tor_Alert_Title),
            warningTitle = warningTitle,
            warningText = getString(R.string.SettingsSecurity_AppRestartWarning),
            actionButtonTitle = actionButton,
            transparentButtonTitle = getString(R.string.Alert_Cancel),
            fragmentManager = childFragmentManager,
            listener = object : ConfirmationDialog.Listener {
                override fun onActionButtonClick() {
                    torViewModel.setTorEnabled()
                }

                override fun onTransparentButtonClick() {
                    torViewModel.resetSwitch()
                }

                override fun onCancelButtonClick() {
                    torViewModel.resetSwitch()
                }
            }
        )
    }

    private fun restartApp() {
        activity?.let {
            MainModule.startAsNewTask(it)
            exitProcess(0)
        }
    }
}

@Composable
private fun SecurityCenterScreen(
    passcodeViewModel: SecurityPasscodeSettingsViewModel,
    torViewModel: SecurityTorSettingsViewModel,
    navController: NavController,
    showAppRestartAlert: () -> Unit,
    restartApp: () -> Unit,
) {

    if (torViewModel.restartApp) {
        restartApp()
        torViewModel.appRestarted()
    }

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                TranslatableString.ResString(R.string.Settings_SecurityCenter),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
            ) {

                item {
                    PasscodeBlock(
                        passcodeViewModel,
                        navController
                    )
                }

                item {
                    Spacer(Modifier.height(24.dp))
                    HeaderText(stringResource(R.string.SecurityCenter_Internet))
                    TorBlock(
                        torViewModel,
                        showAppRestartAlert,
                    )
                }
            }
        }
    }

}

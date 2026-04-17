package cash.p.terminal.modules.settings.security

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.authorizedAction
import cash.p.terminal.core.ensurePinSet
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.modules.settings.security.passcode.SecuritySettingsViewModel
import cash.p.terminal.modules.settings.security.ui.ManagePasscodeSection

@Composable
fun DuressPasscodeBlock(
    viewModel: SecuritySettingsViewModel,
    navController: NavController
) {
    val uiState = viewModel.uiState

    ManagePasscodeSection(
        iconRes = R.drawable.ic_switch_wallet_24,
        enabled = uiState.duressPinEnabled,
        editTextRes = R.string.SettingsSecurity_EditDuressPin,
        enableTextRes = R.string.SettingsSecurity_SetDuressPin,
        disableTextRes = R.string.SettingsSecurity_DisableDuressPin,
        onManageClick = {
            if (uiState.pinEnabled) {
                navController.authorizedAction {
                    if (uiState.duressPinEnabled) {
                        navController.slideFromRight(R.id.editDuressPinFragment)
                    } else {
                        navController.slideFromRight(R.id.setDuressPinIntroFragment)
                    }
                }
            } else {
                navController.ensurePinSet(R.string.PinSet_ForDuress) {
                    navController.slideFromRight(R.id.setDuressPinIntroFragment)
                }
            }
        },
        onDisableClick = {
            navController.authorizedAction {
                viewModel.disableDuressPin()
            }
        }
    )
}

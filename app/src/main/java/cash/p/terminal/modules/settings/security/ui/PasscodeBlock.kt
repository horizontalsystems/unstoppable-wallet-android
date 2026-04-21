package cash.p.terminal.modules.settings.security.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.authorizedAction
import cash.p.terminal.core.ensurePinSet
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.modules.settings.security.SecurityCenterCell
import cash.p.terminal.modules.settings.security.passcode.SecuritySettingsViewModel
import cash.p.terminal.modules.settings.main.HsSettingCell
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HsSwitch
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun PasscodeBlock(
    viewModel: SecuritySettingsViewModel,
    navController: NavController,
) {
    val uiState = viewModel.uiState

    VSpacer(height = 8.dp)
    ManagePasscodeSection(
        iconRes = R.drawable.ic_passcode,
        enabled = uiState.pinEnabled,
        editTextRes = R.string.SettingsSecurity_EditPin,
        showWarningWhenDisabled = true,
        onManageClick = {
            if (!uiState.pinEnabled) {
                navController.slideFromRight(R.id.setPinFragment)
            } else {
                navController.authorizedAction {
                    navController.slideFromRight(R.id.editPinFragment)
                }
            }
        },
        onDisableClick = {
            navController.authorizedAction {
                viewModel.disablePin()
            }
        }
    )

    if (uiState.pinEnabled) {
        VSpacer(32.dp)
        CellUniversalLawrenceSection(
            listOf {
                HsSettingCell(
                    R.string.Settings_AutoLock,
                    R.drawable.ic_lock_20,
                    value = stringResource(uiState.autoLockIntervalName),
                    onClick = {
                        navController.slideFromRight(R.id.autoLockIntervalsFragment)
                    }
                )
            }
        )
    }

    if (viewModel.biometricSettingsVisible) {
        Spacer(Modifier.height(32.dp))
        CellUniversalLawrenceSection {
            SecurityCenterCell(
                start = {
                    Icon(
                        painter = painterResource(R.drawable.icon_touch_id_24),
                        tint = ComposeAppTheme.colors.grey,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                },
                center = {
                    body_leah(
                        text = stringResource(R.string.SettingsSecurity_Biometric_Authentication),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                end = {
                    HsSwitch(
                        checked = uiState.biometricsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                navController.ensurePinSet(R.string.PinSet_ForBiometrics) {
                                    viewModel.enableBiometrics()
                                }
                            } else {
                                viewModel.disableBiometrics()
                            }
                        },
                    )
                }
            )
        }
    }
}

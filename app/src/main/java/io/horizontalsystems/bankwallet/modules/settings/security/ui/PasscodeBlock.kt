package io.horizontalsystems.bankwallet.modules.settings.security.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.pin.SetPinScreen
import io.horizontalsystems.bankwallet.modules.settings.main.HsSettingCell
import io.horizontalsystems.bankwallet.modules.settings.security.SecurityCenterCell
import io.horizontalsystems.bankwallet.modules.settings.security.autolock.AutoLockIntervalsScreen
import io.horizontalsystems.bankwallet.core.authorizedAction
import io.horizontalsystems.bankwallet.core.ensurePinSet
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.settings.security.passcode.SecuritySettingsViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs

@Composable
fun PasscodeBlock(
    viewModel: SecuritySettingsViewModel,
    backStack: NavBackStack<HSScreen>,
) {
    val uiState = viewModel.uiState

    VSpacer(height = 8.dp)
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        CellPrimary(
            middle = {
                val text =
                    if (uiState.pinEnabled) R.string.SettingsSecurity_EditPin else R.string.SettingsSecurity_EnablePin
                CellMiddleInfo(title = stringResource(text).hs)
            },
            right = {
                CellRightNavigation(
                    subtitle = "".hs,
                    icon = if (!uiState.pinEnabled) {
                        painterResource(id = R.drawable.ic_attention_red_20)
                    } else null,
                    iconTint = ComposeAppTheme.colors.lucian
                )
            },
            onClick = {
                if (!uiState.pinEnabled) navController.slideFromRight(R.id.setPinFragment)
                else navController.authorizedAction { navController.slideFromRight(R.id.editPinFragment) }
            }
        )
        if (uiState.pinEnabled) {
            HsDivider()
            CellPrimary(
                middle = {
                    CellMiddleInfo(
                        title = stringResource(R.string.SettingsSecurity_DisablePin).hs(
                            ComposeAppTheme.colors.lucian
                        )
                    )
                },
                onClick = {
//                    TODO("xxx nav3")
//                    navController.authorizedAction { viewModel.disablePin() }
                }
            )
        }
    }

    if (viewModel.biometricSettingsVisible) {
        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ComposeAppTheme.colors.lawrence)
        ) {
            CellPrimary(
                middle = {
                    CellMiddleInfo(title = stringResource(R.string.SettingsSecurity_Biometric_Authentication).hs)
                },
                right = {
                    HsSwitch(
                        checked = uiState.biometricsEnabled,
                        onCheckedChange = { enabled ->
//                            TODO("xxx nav3")
//                            if (enabled) navController.ensurePinSet(R.string.PinSet_ForBiometrics) { viewModel.enableBiometrics() }
//                            else viewModel.disableBiometrics()
                        },
                    )
                }
            )
        }
    }

}

package io.horizontalsystems.walletkit.modules.settings.security.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.pin.SetPinPage
import io.horizontalsystems.walletkit.modules.settings.security.passcode.SecuritySettingsViewModel
import io.horizontalsystems.walletkit.ui.compose.components.HSpacer
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
import io.horizontalsystems.walletkit.uiv3.TestTags
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightControlsSwitcher
import io.horizontalsystems.walletkit.uiv3.components.cell.hs

@Composable
fun PasscodeBlock(
    viewModel: SecuritySettingsViewModel,
    navigation: HSNavigation,
) {
    val uiState = viewModel.uiState

    val disablePin = navigation.authorizedAction { viewModel.disablePin() }
    // Both switches only start a flow; they turn on once the view model reports the new state.
    val togglePin = {
        if (uiState.pinEnabled) disablePin() else navigation.slideFromRight(SetPinPage())
    }
    val enableBiometricsFlow = navigation.ensurePinSet(R.string.PinSet_ForBiometrics) {
        viewModel.enableBiometrics()
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        CellPrimary(
            modifier = Modifier.testTag(TestTags.SECURITY_PASSCODE),
            middle = {
                CellMiddleInfo(title = stringResource(R.string.SettingsSecurity_Passcode).hs)
            },
            right = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!uiState.pinEnabled) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(R.drawable.warning_filled_24),
                            contentDescription = null,
                            tint = ComposeAppTheme.colors.lucian
                        )
                        HSpacer(12.dp)
                    }
                    CellRightControlsSwitcher(
                        checked = uiState.pinEnabled,
                        confirmChange = {
                            togglePin()
                            false
                        },
                        onCheckedChange = {}
                    )
                }
            },
            onClick = togglePin
        )
        if (viewModel.biometricSettingsVisible) {
            HsDivider()
            CellPrimary(
                middle = {
                    CellMiddleInfo(title = stringResource(R.string.SettingsSecurity_Biometric_Authentication).hs)
                },
                right = {
                    CellRightControlsSwitcher(
                        modifier = Modifier.testTag(TestTags.SECURITY_BIOMETRICS),
                        checked = uiState.biometricsEnabled,
                        confirmChange = {
                            if (uiState.biometricsEnabled) {
                                viewModel.disableBiometrics()
                            } else {
                                enableBiometricsFlow()
                            }
                            false
                        },
                        onCheckedChange = {}
                    )
                }
            )
        }
    }
}

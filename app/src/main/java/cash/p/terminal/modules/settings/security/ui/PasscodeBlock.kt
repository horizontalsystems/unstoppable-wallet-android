package cash.p.terminal.modules.settings.security.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
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
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.modules.settings.security.SecurityCenterCell
import cash.p.terminal.modules.settings.security.passcode.SecuritySettingsViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.HsSwitch
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.body_jacob
import cash.p.terminal.ui.compose.components.body_leah
import cash.p.terminal.ui.compose.components.body_lucian

@Composable
fun PasscodeBlock(
    viewModel: SecuritySettingsViewModel,
    navController: NavController,
) {
    val uiState = viewModel.uiState

    VSpacer(height = 8.dp)
    CellUniversalLawrenceSection(buildList<@Composable () -> Unit> {
        add {
            SecurityCenterCell(
                start = {
                    Icon(
                        painter = painterResource(R.drawable.ic_passcode),
                        tint = ComposeAppTheme.colors.jacob,
                        modifier = Modifier.size(24.dp),
                        contentDescription = null,
                    )
                },
                center = {
                    val text = if (uiState.pinEnabled) {
                        R.string.SettingsSecurity_EditPin
                    } else {
                        R.string.SettingsSecurity_EnablePin
                    }
                    body_jacob(
                        text = stringResource(text),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                end = {
                    if (!uiState.pinEnabled) {
                        Image(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(id = R.drawable.ic_attention_red_20),
                            contentDescription = null,
                        )
                    }
                },
                onClick = {
                    if (!uiState.pinEnabled) {
                        navController.slideFromRight(R.id.setPinFragment)
                    } else {
                        navController.authorizedAction {
                            navController.slideFromRight(R.id.editPinFragment)
                        }
                    }
                }
            )
        }
        if (uiState.pinEnabled) {
            add {
                SecurityCenterCell(
                    start = {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete_20),
                            tint = ComposeAppTheme.colors.lucian,
                            modifier = Modifier.size(24.dp),
                            contentDescription = null,
                        )
                    },
                    center = {
                        body_lucian(
                            text = stringResource(R.string.SettingsSecurity_DisablePin),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = {
                        navController.authorizedAction {
                            viewModel.disablePin()
                        }
                    }
                )
            }
        }
    })

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
                                navController.ensurePinSet(R.string.PinSet_ForDuress) {
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

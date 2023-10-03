package io.horizontalsystems.bankwallet.modules.settings.security.ui

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
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.authorizedAction
import io.horizontalsystems.bankwallet.core.ensurePinSet
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.settings.main.HsSettingCell
import io.horizontalsystems.bankwallet.modules.settings.security.SecurityCenterCell
import io.horizontalsystems.bankwallet.modules.settings.security.passcode.SecuritySettingsViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.body_lucian

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

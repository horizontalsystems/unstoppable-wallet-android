package io.horizontalsystems.bankwallet.modules.settings.security.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.settings.security.passcode.SecurityPasscodeSettingsViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.pin.PinModule

@Composable
fun PasscodeBlock(
    viewModel: SecurityPasscodeSettingsViewModel,
    navController: NavController,
) {

    Spacer(Modifier.height(12.dp))

    val blocks = mutableListOf<@Composable () -> Unit>().apply {
        add {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_passcode),
                    tint = ComposeAppTheme.colors.grey,
                    contentDescription = null,
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.SettingsSecurity_EnablePin),
                    style = ComposeAppTheme.typography.body,
                    color = ComposeAppTheme.colors.leah,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                if (!viewModel.pinEnabled) {
                    Image(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(id = R.drawable.ic_attention_red_20),
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(16.dp))
                }
                HsSwitch(
                    checked = viewModel.pinEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            navController.slideFromRight(
                                R.id.pinFragment,
                                PinModule.forSetPin()
                            )
                        } else {
                            navController.slideFromRight(
                                R.id.pinFragment,
                                PinModule.forUnlock()
                            )
                        }
                    }
                )
            }
        }
        if (viewModel.pinEnabled) {
            add {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            navController.slideFromRight(
                                R.id.pinFragment,
                                PinModule.forEditPin()
                            )
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.SettingsSecurity_EditPin),
                        style = ComposeAppTheme.typography.body,
                        color = ComposeAppTheme.colors.leah,
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        tint = ComposeAppTheme.colors.grey,
                        contentDescription = null,
                    )
                }
            }
        }
    }

    CellSingleLineLawrenceSection(blocks)

    if (viewModel.biometricSettingsVisible) {
        Spacer(Modifier.height(32.dp))
        CellSingleLineLawrenceSection(
            listOf {
                Row(
                    modifier = Modifier
                        .clickable {
                            viewModel.setBiometricAuth(!viewModel.biometricEnabled)
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fingerprint),
                        tint = ComposeAppTheme.colors.grey,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.SettingsSecurity_Biometric_Authentication),
                        style = ComposeAppTheme.typography.body,
                        color = ComposeAppTheme.colors.leah,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    HsSwitch(
                        checked = viewModel.biometricEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.setBiometricAuth(enabled)
                        },
                    )
                }
            }
        )
    }
}
package io.horizontalsystems.bankwallet.modules.settings.security

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
import io.horizontalsystems.bankwallet.core.paidAction
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatPremiumTrigger
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.settings.security.passcode.SecuritySettingsViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.PremiumHeader
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.body_lucian
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionPremiumUniversalLawrence
import io.horizontalsystems.subscriptions.core.RobberyProtection

@Composable
fun DuressPasscodeBlock(
    viewModel: SecuritySettingsViewModel,
    navController: NavController
) {
    val uiState = viewModel.uiState
    PremiumHeader()
    SectionPremiumUniversalLawrence {
        SecurityCenterCell(
            start = {
                Icon(
                    painter = painterResource(R.drawable.ic_switch_wallet_24),
                    tint = ComposeAppTheme.colors.jacob,
                    modifier = Modifier.size(24.dp),
                    contentDescription = null,
                )
            },
            center = {
                val text = if (uiState.duressPinEnabled) {
                    R.string.SettingsSecurity_EditDuressPin
                } else {
                    R.string.SettingsSecurity_SetDuressPin
                }
                body_leah(
                    text = stringResource(text),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            onClick = {
                navController.paidAction(RobberyProtection) {
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
                }
                stat(
                    page = StatPage.Security,
                    event = StatEvent.OpenPremium(StatPremiumTrigger.DuressMode)
                )
            }
        )
        if (uiState.duressPinEnabled) {
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
                        text = stringResource(R.string.SettingsSecurity_DisableDuressPin),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                onClick = {
                    navController.authorizedAction {
                        viewModel.disableDuressPin()
                    }
                }
            )
        }
    }
}

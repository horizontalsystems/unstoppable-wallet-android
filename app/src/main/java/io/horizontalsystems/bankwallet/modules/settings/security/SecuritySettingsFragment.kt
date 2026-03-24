package io.horizontalsystems.bankwallet.modules.settings.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.addFromBottom
import io.horizontalsystems.bankwallet.core.addFromRight
import io.horizontalsystems.bankwallet.core.authorizedAction
import io.horizontalsystems.bankwallet.core.ensurePinSet
import io.horizontalsystems.bankwallet.core.paidAction
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatPremiumTrigger
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.pin.EditDuressPinFragment
import io.horizontalsystems.bankwallet.modules.pin.SetDuressPinIntroFragment
import io.horizontalsystems.bankwallet.modules.premium.DefenseSystemFeatureDialog
import io.horizontalsystems.bankwallet.modules.premium.PremiumFeature
import io.horizontalsystems.bankwallet.modules.settings.security.autolock.AutoLockIntervalsFragment
import io.horizontalsystems.bankwallet.modules.settings.security.passcode.SecurityPasscodeSettingsModule
import io.horizontalsystems.bankwallet.modules.settings.security.passcode.SecuritySettingsViewModel
import io.horizontalsystems.bankwallet.modules.settings.security.securesend.SecureSendConfigDialog
import io.horizontalsystems.bankwallet.modules.settings.security.ui.PasscodeBlock
import io.horizontalsystems.bankwallet.modules.usersubscription.BuySubscriptionModel.descriptionStringRes
import io.horizontalsystems.bankwallet.modules.usersubscription.BuySubscriptionModel.titleStringRes
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionPremiumUniversalLawrence
import io.horizontalsystems.bankwallet.uiv3.components.BoxBordered
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightControlsSwitcher
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonStyle
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSIconButton
import io.horizontalsystems.bankwallet.uiv3.components.section.SectionHeader
import io.horizontalsystems.subscriptions.core.RobberyProtection
import io.horizontalsystems.subscriptions.core.SecureSend
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager

class SecuritySettingsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        SecurityCenterScreen(
            securitySettingsViewModel = viewModel(factory = SecurityPasscodeSettingsModule.Factory()),
            navController = navController,
        )
    }

}

@Composable
private fun SecurityCenterScreen(
    securitySettingsViewModel: SecuritySettingsViewModel,
    navController: NavBackStack<HSScreen>,
) {
    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        securitySettingsViewModel.update()
    }

    val uiState = securitySettingsViewModel.uiState

    HSScaffold(
        title = stringResource(R.string.Settings_SecurityCenter),
        onBack = navController::removeLastOrNull,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState())
        ) {
            PasscodeBlock(
                securitySettingsViewModel,
                navController
            )

            VSpacer(24.dp)
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ComposeAppTheme.colors.lawrence)
            ) {
                if (uiState.pinEnabled) {
                    CellPrimary(
                        middle = {
                            CellMiddleInfo(
                                title = stringResource(R.string.Settings_AutoLock).hs,
                                subtitle = stringResource(R.string.Settings_AutoLock_Description).hs
                            )
                        },
                        right = {
                            CellRightNavigation(subtitle = stringResource(uiState.autoLockIntervalName).hs)
                        },
                        onClick = { navController.addFromRight(AutoLockIntervalsFragment()) }
                    )
                    HsDivider()
                }
                CellPrimary(
                    middle = {
                        CellMiddleInfo(
                            title = stringResource(id = R.string.Appearance_BalanceAutoHide).hs,
                            subtitle = stringResource(R.string.Appearance_BalanceAutoHide_Description).hs
                        )
                    },
                    right = {
                        CellRightControlsSwitcher(
                            checked = uiState.balanceAutoHideEnabled,
                            onCheckedChange = {
                                securitySettingsViewModel.onSetBalanceAutoHidden(it)
                            }
                        )
                    },
                )
                HsDivider()
                CellPrimary(
                    middle = {
                        CellMiddleInfo(
                            title = stringResource(id = R.string.SettingsSecurity_HideSuspiciousTxs).hs,
                            subtitle = stringResource(R.string.SettingsSecurity_HideSuspiciousTxs_Description).hs
                        )
                    },
                    right = {
                        CellRightControlsSwitcher(
                            checked = uiState.hideSuspiciousTxs,
                            onCheckedChange = {
                                securitySettingsViewModel.hideSuspiciousTxs(it)
                            }
                        )
                    },
                )
            }

            VSpacer(height = 6.dp)

            SectionHeader(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = stringResource(R.string.Premium_DefenseSystem),
                icon = R.drawable.defense_gradient_filled_24
            )

            SectionPremiumUniversalLawrence {
                uiState.defenseSystemActions.forEachIndexed { i, defenseAction ->
                    val action = defenseAction.action
                    BoxBordered(top = i != 0) {
                        CellPrimary(
                            middle = {
                                CellMiddleInfo(
                                    title = stringResource(action.titleStringRes).hs,
                                    subtitle = stringResource(action.descriptionStringRes).hs
                                )
                            },
                            right = {
                                CellRightControlsSwitcher(
                                    checked = defenseAction.enabled,
                                    confirmChange = {
                                        if (UserSubscriptionManager.isActionAllowed(action)) {
                                            if (action == SecureSend) {
                                                navController.addFromBottom(SecureSendConfigDialog())
                                                false
                                            } else {
                                                true
                                            }
                                        } else {
                                            navController.slideFromBottom(
                                                DefenseSystemFeatureDialog(DefenseSystemFeatureDialog.Input(PremiumFeature.getFeature(action))),
                                            )
                                            false
                                        }
                                    }
                                ) {
                                    securitySettingsViewModel.setActionEnabled(action, it)
                                }
                            },
                            onClick = if (action == SecureSend) {
                                {
                                    navController.addFromBottom(SecureSendConfigDialog())
                                }
                            } else {
                                null
                            }
                        )
                    }
                }

                BoxBordered(top = true) {
                    CellPrimary(
                        middle = {
                            CellMiddleInfo(
                                title = stringResource(R.string.Premium_UpgradeFeature_RobberyProtection).hs,
                                subtitle = stringResource(R.string.Premium_UpgradeFeature_RobberyProtection_Description).hs
                            )
                        },
                        right = {
                            val onClick = {
                                navController.paidAction(RobberyProtection) {
                                    if (uiState.pinEnabled) {
                                        navController.authorizedAction {
                                            if (uiState.duressPinEnabled) {
                                                navController.addFromRight(EditDuressPinFragment())
                                            } else {
                                                navController.addFromRight(SetDuressPinIntroFragment())
                                            }
                                        }
                                    } else {
                                        navController.ensurePinSet(R.string.PinSet_ForDuress) {
                                            navController.addFromRight(SetDuressPinIntroFragment())
                                        }
                                    }
                                }
                                stat(
                                    page = StatPage.Security,
                                    event = StatEvent.OpenPremium(StatPremiumTrigger.DuressMode)
                                )
                            }

                            if (uiState.duressPinEnabled) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    HSIconButton(
                                        variant = ButtonVariant.Secondary,
                                        style = ButtonStyle.Solid,
                                        size = ButtonSize.Small,
                                        icon = painterResource(R.drawable.ic_edit_24),
                                        onClick = onClick
                                    )

                                    HSIconButton(
                                        variant = ButtonVariant.Secondary,
                                        style = ButtonStyle.Solid,
                                        size = ButtonSize.Small,
                                        icon = painterResource(R.drawable.trash_24)
                                    ) {
                                        navController.authorizedAction {
                                            securitySettingsViewModel.disableDuressPin()
                                        }
                                    }
                                }
                            } else {
                                HSButton(
                                    variant = ButtonVariant.Secondary,
                                    style = ButtonStyle.Solid,
                                    size = ButtonSize.Small,
                                    title = stringResource(R.string.Button_Add),
                                    onClick = onClick
                                )
                            }
                        }
                    )
                }
            }

            VSpacer(height = 32.dp)
        }
    }
}

@Composable
fun SecurityCenterCell(
    start: @Composable RowScope.() -> Unit,
    center: @Composable RowScope.() -> Unit,
    end: @Composable() (RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    RowUniversal(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .height(48.dp),
        verticalPadding = 0.dp,
        onClick = onClick
    ) {
        start.invoke(this)
        Spacer(Modifier.width(16.dp))
        center.invoke(this)
        end?.let {
            Spacer(
                Modifier
                    .defaultMinSize(minWidth = 8.dp)
                    .weight(1f)
            )
            end.invoke(this)
        }
    }
}
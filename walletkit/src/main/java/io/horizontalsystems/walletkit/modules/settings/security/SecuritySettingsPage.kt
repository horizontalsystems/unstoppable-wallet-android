package io.horizontalsystems.walletkit.modules.settings.security

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
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.pin.EditDuressPinPage
import io.horizontalsystems.walletkit.modules.pin.SetDuressPinIntroPage
import io.horizontalsystems.walletkit.modules.settings.security.autolock.AutoLockIntervalsPage
import io.horizontalsystems.walletkit.modules.settings.security.passcode.SecurityPasscodeSettingsModule
import io.horizontalsystems.walletkit.modules.settings.security.passcode.SecuritySettingsViewModel
import io.horizontalsystems.walletkit.modules.settings.security.securesend.SecureSendConfigSheet
import io.horizontalsystems.walletkit.modules.settings.security.ui.PasscodeBlock
import io.horizontalsystems.walletkit.modules.usersubscription.BuySubscriptionHavHostPage
import io.horizontalsystems.walletkit.modules.usersubscription.BuySubscriptionModel.descriptionStringRes
import io.horizontalsystems.walletkit.modules.usersubscription.BuySubscriptionModel.titleStringRes
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
import io.horizontalsystems.walletkit.ui.compose.components.RowUniversal
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.cell.SectionPremiumUniversalLawrence
import io.horizontalsystems.walletkit.uiv3.components.BoxBordered
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightControlsSwitcher
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonSize
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonStyle
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.walletkit.uiv3.components.controls.HSButton
import io.horizontalsystems.walletkit.uiv3.components.controls.HSIconButton
import io.horizontalsystems.walletkit.uiv3.components.section.SectionHeader
import io.horizontalsystems.subscriptions.core.SecureSend
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.serialization.Serializable

@Serializable
data object SecuritySettingsPage : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        SecurityCenterScreen(
            securitySettingsViewModel = viewModel(factory = SecurityPasscodeSettingsModule.Factory()),
            navigation = navigation,
        )
    }

}

@Composable
private fun SecurityCenterScreen(
    securitySettingsViewModel: SecuritySettingsViewModel,
    navigation: HSNavigation,
) {
    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        securitySettingsViewModel.update()
    }

    val uiState = securitySettingsViewModel.uiState

    HSScaffold(
        title = stringResource(R.string.Settings_SecurityCenter),
        onBack = navigation::removeLastOrNull,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState())
        ) {
            PasscodeBlock(
                securitySettingsViewModel,
                navigation
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
                        onClick = { navigation.slideFromRight(AutoLockIntervalsPage) }
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
                                                navigation.slideFromBottom(SecureSendConfigSheet)
                                                false
                                            } else {
                                                true
                                            }
                                        } else {
                                            navigation.slideFromBottom(BuySubscriptionHavHostPage)
                                            false
                                        }
                                    }
                                ) {
                                    securitySettingsViewModel.setActionEnabled(action, it)
                                }
                            },
                            onClick = if (action == SecureSend) {
                                {
                                    navigation.slideFromBottom(SecureSendConfigSheet)
                                }
                            } else {
                                null
                            }
                        )
                    }
                }

            }

            VSpacer(height = 24.dp)

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ComposeAppTheme.colors.lawrence)
            ) {
                val authorizedActionDuressPin = navigation.authorizedAction {
                    if (uiState.duressPinEnabled) {
                        navigation.slideFromRight(EditDuressPinPage)
                    } else {
                        navigation.slideFromRight(SetDuressPinIntroPage)
                    }
                }

                val setDuressPinFlow = navigation.ensurePinSet(R.string.PinSet_ForDuress) {
                    navigation.slideFromRight(SetDuressPinIntroPage)
                }

                CellPrimary(
                    middle = {
                        CellMiddleInfo(
                            title = stringResource(R.string.Premium_UpgradeFeature_RobberyProtection).hs,
                            subtitle = stringResource(R.string.Premium_UpgradeFeature_RobberyProtection_Description).hs
                        )
                    },
                    right = {
                        val onClick = {
                            if (uiState.pinEnabled) {
                                authorizedActionDuressPin()
                            } else {
                                setDuressPinFlow()
                            }
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
                                    icon = painterResource(R.drawable.trash_24),
                                    onClick = navigation.authorizedAction {
                                        securitySettingsViewModel.disableDuressPin()
                                    }
                                )
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
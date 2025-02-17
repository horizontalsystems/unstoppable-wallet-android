package cash.p.terminal.modules.settings.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.authorizedAction
import cash.p.terminal.core.ensurePinSet
import cash.p.terminal.modules.main.MainModule
import cash.p.terminal.modules.pin.ConfirmPinFragment
import cash.p.terminal.modules.pin.PinType
import cash.p.terminal.modules.pin.SetPinFragment
import cash.p.terminal.modules.settings.security.passcode.SecurityPasscodeSettingsModule
import cash.p.terminal.modules.settings.security.passcode.SecuritySettingsViewModel
import cash.p.terminal.modules.settings.security.tor.SecurityTorSettingsModule
import cash.p.terminal.modules.settings.security.tor.SecurityTorSettingsViewModel
import cash.p.terminal.modules.settings.security.ui.PasscodeBlock
import cash.p.terminal.modules.settings.security.ui.TorBlock
import cash.p.terminal.modules.settings.security.ui.TransactionAutoHideBlock
import cash.p.terminal.modules.settings.security.ui.TransferPasscodeBlock
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.ui.compose.components.HsSwitch
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui.extensions.ConfirmationDialog
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import kotlin.system.exitProcess

class SecuritySettingsFragment : BaseComposeFragment() {

    private val torViewModel by viewModels<SecurityTorSettingsViewModel> {
        SecurityTorSettingsModule.Factory()
    }

    private val securitySettingsViewModel by viewModels<SecuritySettingsViewModel> {
        SecurityPasscodeSettingsModule.Factory()
    }

    @Composable
    override fun GetContent(navController: NavController) {
        SecurityCenterScreen(
            securitySettingsViewModel = securitySettingsViewModel,
            torViewModel = torViewModel,
            navController = navController,
            onTransactionAutoHideEnabledChange = { enabled ->
                if (enabled) {
                    if (securitySettingsViewModel.uiState.pinEnabled) {
                        securitySettingsViewModel.onTransactionAutoHideEnabledChange(true)
                    } else {
                        navController.ensurePinSet(R.string.PinSet_Title) {
                            securitySettingsViewModel.onTransactionAutoHideEnabledChange(true)
                        }
                    }
                } else {
                    navController.authorizedAction(
                        ConfirmPinFragment.InputConfirm(
                            descriptionResId = R.string.Unlock_EnterPasscode_Transactions_Hide,
                            pinType = PinType.TRANSACTIONS_HIDE
                        )
                    ) {
                        securitySettingsViewModel.onTransactionAutoHideEnabledChange(false)
                    }
                }
            },
            onChangeDisplayClicked = {
                navController.authorizedAction(
                    ConfirmPinFragment.InputConfirm(
                        descriptionResId = R.string.Unlock_EnterPasscode_Transactions_Hide,
                        pinType = PinType.TRANSACTIONS_HIDE
                    )
                ) {
                    navController.slideFromRight(R.id.chooseDisplayTransactionsFragment)
                }
            },
            onSetTransactionAutoHidePinClicked = {
                if (!securitySettingsViewModel.uiState.transactionAutoHideSeparatePinExists) {
                    navController.authorizedAction(
                        ConfirmPinFragment.InputConfirm(
                            descriptionResId = R.string.Unlock_EnterPasscode,
                            pinType = PinType.REGULAR
                        )
                    ) {
                        navController.slideFromRight(
                            R.id.setPinFragment,
                            SetPinFragment.Input(
                                descriptionResId = R.string.PinSet_Transactions_Hide,
                                pinType = PinType.TRANSACTIONS_HIDE
                            )
                        )
                    }
                } else {
                    navController.authorizedAction(
                        ConfirmPinFragment.InputConfirm(
                            descriptionResId = R.string.Unlock_EnterPasscode_Transactions_Hide,
                            pinType = PinType.TRANSACTIONS_HIDE
                        )
                    ) {
                        navController.slideFromRight(
                            resId = R.id.editPinFragment,
                            input = SetPinFragment.Input(
                                R.string.PinSet_Transactions_Hide,
                                PinType.TRANSACTIONS_HIDE
                            )
                        )
                    }
                }
            },
            onDisableTransactionAutoHidePinClicked = {
                navController.authorizedAction(
                    ConfirmPinFragment.InputConfirm(
                        descriptionResId = R.string.Unlock_EnterPasscode_Transactions_Hide,
                        pinType = PinType.TRANSACTIONS_HIDE
                    )
                ) {
                    securitySettingsViewModel.onDisableTransactionAutoHidePin()
                }
            },
            showAppRestartAlert = { showAppRestartAlert() },
            restartApp = { restartApp() },
            onTransferPasscodeEnabledChange = { enabled ->
                if (enabled) {
                    if (securitySettingsViewModel.uiState.pinEnabled) {
                        securitySettingsViewModel.onTransferPasscodeEnabledChange(true)
                    } else {
                        navController.ensurePinSet(R.string.PinSet_Title) {
                            securitySettingsViewModel.onTransferPasscodeEnabledChange(true)
                        }
                    }
                } else {
                    navController.authorizedAction(
                        ConfirmPinFragment.InputConfirm(
                            descriptionResId = R.string.Unlock_EnterPasscode_Transfer,
                            pinType = PinType.REGULAR
                        )
                    ) {
                        securitySettingsViewModel.onTransferPasscodeEnabledChange(false)
                    }
                }
            }
        )
    }

    private fun showAppRestartAlert() {
        val warningTitle = if (torViewModel.torCheckEnabled) {
            getString(R.string.Tor_Connection_Enable)
        } else {
            getString(R.string.Tor_Connection_Disable)
        }

        val actionButton = if (torViewModel.torCheckEnabled) {
            getString(R.string.Button_Enable)
        } else {
            getString(R.string.Button_Disable)
        }

        ConfirmationDialog.show(
            icon = R.drawable.ic_tor_connection_24,
            title = getString(R.string.Tor_Alert_Title),
            warningTitle = warningTitle,
            warningText = getString(R.string.SettingsSecurity_AppRestartWarning),
            actionButtonTitle = actionButton,
            transparentButtonTitle = getString(R.string.Alert_Cancel),
            fragmentManager = childFragmentManager,
            listener = object : ConfirmationDialog.Listener {
                override fun onActionButtonClick() {
                    torViewModel.setTorEnabled()
                }

                override fun onTransparentButtonClick() {
                    torViewModel.resetSwitch()
                }

                override fun onCancelButtonClick() {
                    torViewModel.resetSwitch()
                }
            }
        )
    }

    private fun restartApp() {
        activity?.let {
            MainModule.startAsNewTask(it)
            exitProcess(0)
        }
    }
}

@Composable
private fun SecurityCenterScreen(
    securitySettingsViewModel: SecuritySettingsViewModel,
    torViewModel: SecurityTorSettingsViewModel,
    navController: NavController,
    onTransactionAutoHideEnabledChange: (Boolean) -> Unit,
    onSetTransactionAutoHidePinClicked: () -> Unit,
    onDisableTransactionAutoHidePinClicked: () -> Unit,
    onChangeDisplayClicked: () -> Unit,
    onTransferPasscodeEnabledChange: (Boolean) -> Unit,
    showAppRestartAlert: () -> Unit,
    restartApp: () -> Unit,
) {
    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        securitySettingsViewModel.update()
    }

    if (torViewModel.restartApp) {
        restartApp()
        torViewModel.appRestarted()
    }

    val uiState = securitySettingsViewModel.uiState
    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.Settings_SecurityCenter),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
            )
        }
    ) {
        Column(
            Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
        ) {
            PasscodeBlock(
                securitySettingsViewModel,
                navController
            )

            VSpacer(height = 32.dp)

            CellUniversalLawrenceSection {
                SecurityCenterCell(
                    start = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_off_24),
                            tint = ComposeAppTheme.colors.grey,
                            modifier = Modifier.size(24.dp),
                            contentDescription = null
                        )
                    },
                    center = {
                        body_leah(
                            text = stringResource(id = R.string.Appearance_BalanceAutoHide),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    end = {
                        HsSwitch(
                            checked = uiState.balanceAutoHideEnabled,
                            onCheckedChange = {
                                securitySettingsViewModel.onSetBalanceAutoHidden(it)
                            }
                        )
                    }
                )
            }
            InfoText(
                text = stringResource(R.string.Appearance_BalanceAutoHide_Description),
                paddingBottom = 32.dp
            )

            TransactionAutoHideBlock(
                transactionAutoHideEnabled = uiState.transactionAutoHideEnabled,
                displayLevel = uiState.displayLevel,
                transactionAutoHideSeparatePinExists = uiState.transactionAutoHideSeparatePinExists,
                onTransactionAutoHideEnabledChange = onTransactionAutoHideEnabledChange,
                onPinClicked = onSetTransactionAutoHidePinClicked,
                onDisablePinClicked = onDisableTransactionAutoHidePinClicked,
                onChangeDisplayClicked = onChangeDisplayClicked
            )

            TransferPasscodeBlock(
                transferPasscodeEnabled = uiState.transferPasscodeEnabled,
                onTransferPasscodeEnabledChange = onTransferPasscodeEnabledChange
            )

            TorBlock(
                torViewModel,
                showAppRestartAlert,
            )

            DuressPasscodeBlock(
                securitySettingsViewModel,
                navController
            )
            InfoText(text = stringResource(R.string.SettingsSecurity_DuressPinDescription))

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
        modifier = Modifier.padding(horizontal = 16.dp),
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
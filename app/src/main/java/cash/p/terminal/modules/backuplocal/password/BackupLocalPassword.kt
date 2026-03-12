package cash.p.terminal.modules.backuplocal.password

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.premiumAction
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellowWithSpinner
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.FormsInputPassword
import cash.p.terminal.ui_compose.components.HFillSpacer
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.HsIconButton
import cash.p.terminal.ui_compose.components.HsSwitch
import cash.p.terminal.ui_compose.components.HudHelper
import cash.p.terminal.ui_compose.components.InfoBottomSheet
import cash.p.terminal.ui_compose.components.InfoText
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.SnackbarDuration
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun LocalBackupPasswordScreen(
    backupType: BackupType,
    navController: NavController,
    onBackClick: () -> Unit,
    onFinish: () -> Unit
) {
    val viewModel = viewModel<BackupLocalPasswordViewModel>(
        factory = BackupLocalPasswordModule.Factory(backupType)
    )

    val view = LocalView.current
    val context = LocalContext.current
    var hidePassphrase by remember { mutableStateOf(true) }
    var hideDuressPassphrase by remember { mutableStateOf(true) }
    var showDuressInfoSheet by remember { mutableStateOf(false) }
    val uiState = viewModel.uiState

    // Show duress toggle only for full backup (single wallet = one password)
    val showDuressToggle = backupType is BackupType.FullBackup

    val backupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            uri?.let {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    uiState.backupData?.let { backupData ->
                        try {
                            outputStream.write(backupData)
                            outputStream.flush()

                            HudHelper.showSuccessMessage(
                                contenView = view,
                                resId = R.string.LocalBackup_BackupSaved,
                                duration = SnackbarDuration.SHORT,
                                icon = R.drawable.ic_download_24,
                                iconTint = R.color.white
                            )

                            viewModel.backupFinished()
                        } catch (e: Throwable) {
                            HudHelper.showErrorMessage(view, e.message ?: e.javaClass.simpleName)
                        }
                    }
                }
            } ?: run {
                viewModel.backupCanceled()
            }
        }

    if (uiState.error != null) {
        Toast.makeText(App.instance, uiState.error, Toast.LENGTH_SHORT).show()
        onFinish()
        viewModel.accountErrorIsShown()
    }

    if (uiState.backupData != null) {
        App.pinComponent.keepUnlocked()
        try {
            backupLauncher.launch(viewModel.backupFileName)
        } catch (_: ActivityNotFoundException) {
            HudHelper.showErrorMessage(view, R.string.error_no_file_manager)
            viewModel.backupCanceled()
        }
    }

    if (uiState.closeScreen) {
        viewModel.closeScreenCalled()
        onFinish()
    }

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.LocalBackup_SetPassword),
                navigationIcon = {
                    HsBackButton(onClick = onBackClick)
                }
            )
        }
    ) {
        Column(modifier = Modifier.padding(it)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {

                InfoText(text = stringResource(R.string.LocalBackup_ProtextBackupWithPasswordInfo))
                VSpacer(24.dp)
                FormsInputPassword(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    hint = stringResource(R.string.Password),
                    state = uiState.passphraseState,
                    onValueChange = viewModel::onChangePassphrase,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    hide = hidePassphrase,
                    onToggleHide = {
                        hidePassphrase = !hidePassphrase
                    }
                )
                VSpacer(16.dp)
                FormsInputPassword(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    hint = stringResource(R.string.ConfirmPassphrase),
                    state = uiState.passphraseConfirmState,
                    onValueChange = viewModel::onChangePassphraseConfirmation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    hide = hidePassphrase,
                    onToggleHide = {
                        hidePassphrase = !hidePassphrase
                    }
                )
                // Duress backup toggle (only for full backup)
                if (showDuressToggle) {
                    VSpacer(24.dp)
                    DuressBackupToggle(
                        enabled = uiState.duressBackupEnabled,
                        available = uiState.duressBackupAvailable && uiState.pinEnabled,
                        onInfoClick = { showDuressInfoSheet = true },
                        onToggleClick = {
                            // Check premium and then verify PIN
                            navController.premiumAction {
                                viewModel.onDuressBackupToggle(true)
                            }
                        },
                        onToggleOff = {
                            viewModel.onDuressBackupToggle(false)
                        },
                        onDisabledClick = {
                            // Show message that duress mode needs to be configured
                            HudHelper.showErrorMessage(
                                view,
                                context.getString(R.string.local_backup_configure_duress_first)
                            )
                        }
                    )

                    // Duress password fields when enabled
                    if (uiState.duressBackupEnabled) {
                        VSpacer(24.dp)
                        FormsInputPassword(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            hint = stringResource(R.string.local_backup_duress_password),
                            state = uiState.duressPassphraseState,
                            onValueChange = viewModel::onChangeDuressPassphrase,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            hide = hideDuressPassphrase,
                            onToggleHide = {
                                hideDuressPassphrase = !hideDuressPassphrase
                            }
                        )
                        VSpacer(16.dp)
                        FormsInputPassword(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            hint = stringResource(R.string.local_backup_confirm_duress_password),
                            state = uiState.duressPassphraseConfirmState,
                            onValueChange = viewModel::onChangeDuressPassphraseConfirmation,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            hide = hideDuressPassphrase,
                            onToggleHide = {
                                hideDuressPassphrase = !hideDuressPassphrase
                            }
                        )
                    }
                }

                VSpacer(32.dp)
                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.LocalBackup_DontForgetPasswordWarning)
                )
                VSpacer(32.dp)

            }

            // Info bottom sheet for duress backup
            if (showDuressInfoSheet) {
                InfoBottomSheet(
                    title = stringResource(R.string.local_backup_duress_info_title),
                    text = stringResource(R.string.local_backup_duress_info_description),
                    onDismiss = { showDuressInfoSheet = false }
                )
            }
            ButtonsGroupWithShade {
                ButtonPrimaryYellowWithSpinner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.LocalBackup_SaveAndBackup),
                    showSpinner = uiState.showButtonSpinner,
                    enabled = uiState.showButtonSpinner.not(),
                    onClick = {
                        viewModel.onSaveClick()
                    },
                )
            }
        }
    }
}

@Composable
private fun DuressBackupToggle(
    enabled: Boolean,
    available: Boolean,
    onInfoClick: () -> Unit,
    onToggleClick: () -> Unit,
    onToggleOff: () -> Unit,
    onDisabledClick: () -> Unit
) {
    CellUniversalLawrenceSection(listOf {
        RowUniversal(
            modifier = Modifier.padding(horizontal = 16.dp),
            onClick = {
                if (!available) {
                    onDisabledClick()
                }
            },
        ) {
            body_leah(
                text = stringResource(R.string.local_backup_duress_toggle),
            )
            HsIconButton(
                onClick = onInfoClick,
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_info_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }
            HFillSpacer(minWidth = 4.dp)
            HsSwitch(
                checked = enabled,
                onCheckedChange = { isChecked ->
                    if (!available) {
                        onDisabledClick()
                        return@HsSwitch
                    }
                    if (isChecked) {
                        onToggleClick()
                    } else {
                        onToggleOff()
                    }
                }
            )
        }
    })
}

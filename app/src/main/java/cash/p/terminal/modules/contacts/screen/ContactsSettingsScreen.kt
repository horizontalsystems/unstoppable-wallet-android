package cash.p.terminal.modules.contacts.screen

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.core.Caution
import cash.p.terminal.core.openInputStreamSafe
import cash.p.terminal.modules.settings.security.ui.ManagePasscodeSection
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.HsSettingCell
import cash.p.terminal.ui_compose.components.HudHelper
import cash.p.terminal.ui_compose.components.InfoText
import cash.p.terminal.ui_compose.components.PremiumHeader
import cash.p.terminal.ui_compose.components.SnackbarDuration
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsSettingsScreen(
    deleteContactsPinEnabled: Boolean,
    hasContacts: Boolean,
    backupJson: String,
    backupFileName: String,
    onRestore: (String) -> Unit,
    onPrepareForBackup: () -> Unit,
    onManageDeleteContactsPasscode: () -> Unit,
    onDisableDeleteContactsPasscode: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    var showRestoreWarning by remember { mutableStateOf(false) }
    var showDisableConfirmation by remember { mutableStateOf(false) }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.contentResolver.openInputStreamSafe(it)?.use { inputStream ->
                try {
                    inputStream.bufferedReader().use { br ->
                        onRestore(br.readText())
                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done, SnackbarDuration.SHORT)
                    }
                } catch (e: Throwable) {
                    HudHelper.showErrorMessage(view, e.message ?: e.javaClass.simpleName)
                }
            }
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                try {
                    outputStream.bufferedWriter().use { bw ->
                        bw.write(backupJson)
                        bw.flush()
                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done, SnackbarDuration.SHORT)
                    }
                } catch (e: Throwable) {
                    HudHelper.showErrorMessage(view, e.message ?: e.javaClass.simpleName)
                }
            }
        }
    }

    ContactsSettingsContent(
        deleteContactsPinEnabled = deleteContactsPinEnabled,
        onManageDeleteContactsPasscode = onManageDeleteContactsPasscode,
        onDisableDeleteContactsPasscode = { showDisableConfirmation = true },
        onRestoreContacts = {
            if (hasContacts) {
                showRestoreWarning = true
            } else {
                restoreLauncher.launch(arrayOf("application/json"))
            }
        },
        onBackupContacts = {
            onPrepareForBackup()
            try {
                backupLauncher.launch(backupFileName)
            } catch (_: ActivityNotFoundException) {
                HudHelper.showErrorMessage(view, R.string.error_no_file_manager)
            }
        },
        onClose = onClose
    )

    if (showRestoreWarning) {
        ModalBottomSheet(
            onDismissRequest = { showRestoreWarning = false },
            dragHandle = null,
            containerColor = ComposeAppTheme.colors.transparent,
        ) {
            ConfirmationBottomSheet(
                title = stringResource(R.string.Alert_TitleWarning),
                text = stringResource(R.string.Contacts_Restore_Warning),
                iconPainter = painterResource(R.drawable.icon_warning_2_20),
                iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
                confirmText = stringResource(R.string.Contacts_AddAddress_Replace),
                cautionType = Caution.Type.Error,
                cancelText = stringResource(R.string.Button_Cancel),
                onConfirm = {
                    showRestoreWarning = false
                    restoreLauncher.launch(arrayOf("application/json"))
                },
                onClose = { showRestoreWarning = false }
            )
        }
    }

    if (showDisableConfirmation) {
        ModalBottomSheet(
            onDismissRequest = { showDisableConfirmation = false },
            dragHandle = null,
            containerColor = ComposeAppTheme.colors.transparent,
        ) {
            ConfirmationBottomSheet(
                title = stringResource(R.string.SettingsSecurity_DisablePin),
                text = stringResource(R.string.disable_delete_contacts_passcode_warning),
                iconPainter = painterResource(R.drawable.icon_warning_2_20),
                iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
                confirmText = stringResource(R.string.SettingsSecurity_DisablePin),
                cautionType = Caution.Type.Error,
                cancelText = stringResource(R.string.Button_Cancel),
                onConfirm = {
                    showDisableConfirmation = false
                    onDisableDeleteContactsPasscode()
                },
                onClose = { showDisableConfirmation = false }
            )
        }
    }
}

@Composable
private fun ContactsSettingsContent(
    deleteContactsPinEnabled: Boolean,
    onManageDeleteContactsPasscode: () -> Unit,
    onDisableDeleteContactsPasscode: () -> Unit,
    onRestoreContacts: () -> Unit,
    onBackupContacts: () -> Unit,
    onClose: () -> Unit
) {
    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.Settings_Title),
                navigationIcon = {
                    HsBackButton(onClick = onClose)
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            VSpacer(12.dp)
            PremiumHeader()
            ManagePasscodeSection(
                iconRes = R.drawable.ic_passcode,
                enabled = deleteContactsPinEnabled,
                editTextRes = R.string.SettingsSecurity_EditPin,
                onManageClick = onManageDeleteContactsPasscode,
                onDisableClick = onDisableDeleteContactsPasscode
            )
            InfoText(text = stringResource(R.string.pin_set_for_delete_all_contacts))

            VSpacer(16.dp)
            CellUniversalLawrenceSection(
                listOf(
                    {
                        HsSettingCell(
                            title = R.string.Contacts_Restore,
                            onClick = onRestoreContacts
                        )
                    },
                    {
                        HsSettingCell(
                            title = R.string.Contacts_Backup,
                            onClick = onBackupContacts
                        )
                    }
                )
            )
            VSpacer(32.dp)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContactsSettingsScreenPreview() {
    ComposeAppTheme {
        ContactsSettingsContent(
            deleteContactsPinEnabled = true,
            onManageDeleteContactsPasscode = {},
            onDisableDeleteContactsPasscode = {},
            onRestoreContacts = {},
            onBackupContacts = {},
            onClose = {}
        )
    }
}

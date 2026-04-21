package cash.p.terminal.feature.logging.settings

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.feature.logging.R
import cash.p.terminal.feature.logging.components.DeleteLogsConfirmationDialog
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.Select
import cash.p.terminal.ui_compose.components.AlertGroup
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSectionAnimated
import cash.p.terminal.ui_compose.components.HeaderText
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.HsSettingCell
import cash.p.terminal.ui_compose.components.InfoBottomSheet
import cash.p.terminal.ui_compose.components.InfoText
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.RowWithArrow
import cash.p.terminal.ui_compose.components.SectionUniversalLawrence
import cash.p.terminal.ui_compose.components.SwitchWithText
import cash.p.terminal.ui_compose.components.SwitchWithTextWarning
import cash.p.terminal.ui_compose.components.TextImportantError
import cash.p.terminal.ui_compose.components.VFillSpacer
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_grey
import cash.p.terminal.ui_compose.components.body_lucian
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import io.horizontalsystems.core.entities.AutoDeletePeriod
import io.horizontalsystems.core.ui.dialogs.ConfirmationDialogBottomSheet

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LoggingSettingsScreen(
    uiState: LoggingSettingsUiState,
    onLogSuccessfulLoginsToggle: (Boolean) -> Unit,
    onSelfieOnSuccessfulLoginToggle: (Boolean) -> Unit,
    onLogUnsuccessfulLoginsToggle: (Boolean) -> Unit,
    onSelfieOnUnsuccessfulLoginToggle: (Boolean) -> Unit,
    onLogIntoDuressModeToggle: (Boolean) -> Unit,
    onSelfieOnDuressLoginToggle: (Boolean) -> Unit,
    onPasscodeToggle: (Boolean) -> Unit,
    onDeleteAllContactsPasscodeToggle: (Boolean) -> Unit,
    onSendLoginNotificationClick: () -> Unit,
    onDeleteAllAuthDataOnDuressToggle: (Boolean) -> Unit,
    onAutoDeletePeriodChanged: (AutoDeletePeriod) -> Unit,
    onDeleteAllLogs: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    var showPeriodSelector by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showCameraPermissionDeniedDialog by remember { mutableStateOf(false) }

    val cameraPermissionState = if (!isPreview) {
        rememberPermissionState(Manifest.permission.CAMERA) { granted ->
            if (granted) {
                onSelfieOnSuccessfulLoginToggle(true)
            } else {
                showCameraPermissionDeniedDialog = true
            }
        }
    } else {
        null
    }

    val noCameraPermissions = cameraPermissionState?.status != PermissionStatus.Granted

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.login_logging_title),
                navigationIcon = {
                    HsBackButton(onClick = onClose)
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.button_info),
                        icon = R.drawable.ic_info_20,
                        tint = ComposeAppTheme.colors.grey,
                        onClick = { showInfoSheet = true }
                    )
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Subscription expired warning banner
            VSpacer(12.dp)
            @Suppress("ComplexCondition")
            if (!uiState.isPremiumActive &&
                (uiState.logSuccessfulLoginsEnabled ||
                uiState.logUnsuccessfulLoginsEnabled ||
                uiState.logIntoDuressModeEnabled)
            ) {
                TextImportantError(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = stringResource(R.string.login_logging_subscription_expired_title),
                    text = stringResource(R.string.login_logging_subscription_expired_description),
                    icon = R.drawable.icon_24_warning_2
                )
                VSpacer(16.dp)
            }

            // STANDARD LOGIN section
            HeaderText(stringResource(R.string.login_logging_standard_login))
            CellUniversalLawrenceSectionAnimated(
                primaryContent = {
                    SwitchWithText(
                        text = stringResource(R.string.login_logging_log_successful),
                        checked = uiState.logSuccessfulLoginsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !uiState.isPremiumActive) return@SwitchWithText
                            onLogSuccessfulLoginsToggle(enabled)
                        }
                    )
                },
                animatedContent = {
                    SwitchWithTextWarning(
                        text = stringResource(R.string.login_logging_selfie_on_successful_login),
                        checked = uiState.selfieOnSuccessfulLoginEnabled,
                        showWarning = noCameraPermissions && uiState.selfieOnSuccessfulLoginEnabled,
                        onWarningIconClick = { showCameraPermissionDeniedDialog = true },
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (!uiState.isPremiumActive) return@SwitchWithTextWarning
                                when {
                                    cameraPermissionState?.status == PermissionStatus.Granted -> {
                                        onSelfieOnSuccessfulLoginToggle(true)
                                    }

                                    cameraPermissionState?.status?.shouldShowRationale == true -> {
                                        // Permission was denied before, show dialog
                                        showCameraPermissionDeniedDialog = true
                                    }

                                    else -> {
                                        cameraPermissionState?.launchPermissionRequest()
                                    }
                                }
                            } else {
                                onSelfieOnSuccessfulLoginToggle(false)
                            }
                        }
                    )
                },
                animatedVisible = uiState.logSuccessfulLoginsEnabled
            )
            VSpacer(16.dp)
            CellUniversalLawrenceSectionAnimated(
                primaryContent = {
                    SwitchWithText(
                        text = stringResource(R.string.login_logging_log_unsuccessful),
                        checked = uiState.logUnsuccessfulLoginsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !uiState.isPremiumActive) return@SwitchWithText
                            onLogUnsuccessfulLoginsToggle(enabled)
                        }
                    )
                },
                animatedContent = {
                    SwitchWithTextWarning(
                        text = stringResource(R.string.login_logging_selfie_on_unsuccessful_login),
                        checked = uiState.selfieOnUnsuccessfulLoginEnabled,
                        showWarning = noCameraPermissions && uiState.selfieOnUnsuccessfulLoginEnabled,
                        onWarningIconClick = { showCameraPermissionDeniedDialog = true },
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (!uiState.isPremiumActive) return@SwitchWithTextWarning
                                when {
                                    cameraPermissionState?.status == PermissionStatus.Granted -> {
                                        onSelfieOnUnsuccessfulLoginToggle(true)
                                    }

                                    cameraPermissionState?.status?.shouldShowRationale == true -> {
                                        // Permission was denied before, show dialog
                                        showCameraPermissionDeniedDialog = true
                                    }

                                    else -> {
                                        cameraPermissionState?.launchPermissionRequest()
                                    }
                                }
                            } else {
                                onSelfieOnUnsuccessfulLoginToggle(false)
                            }
                        }
                    )
                },
                animatedVisible = uiState.logUnsuccessfulLoginsEnabled
            )
            // DURESS MODE section
            VSpacer(16.dp)
            HeaderText(stringResource(R.string.login_logging_duress_mode))
            CellUniversalLawrenceSectionAnimated(
                primaryContent = {
                    SwitchWithText(
                        text = stringResource(R.string.login_logging_log_into_duress),
                        checked = uiState.logIntoDuressModeEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !uiState.isPremiumActive) return@SwitchWithText
                            onLogIntoDuressModeToggle(enabled)
                        }
                    )
                },
                animatedContent = {
                    SwitchWithTextWarning(
                        text = stringResource(R.string.login_logging_selfie_on_duress_login),
                        checked = uiState.selfieOnDuressLoginEnabled,
                        showWarning = noCameraPermissions && uiState.selfieOnDuressLoginEnabled,
                        onWarningIconClick = { showCameraPermissionDeniedDialog = true },
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (!uiState.isPremiumActive) return@SwitchWithTextWarning
                                when {
                                    cameraPermissionState?.status == PermissionStatus.Granted -> {
                                        onSelfieOnDuressLoginToggle(true)
                                    }

                                    cameraPermissionState?.status?.shouldShowRationale == true -> {
                                        // Permission was denied before, show dialog
                                        showCameraPermissionDeniedDialog = true
                                    }

                                    else -> {
                                        cameraPermissionState?.launchPermissionRequest()
                                    }
                                }
                            } else {
                                onSelfieOnDuressLoginToggle(false)
                            }
                        }
                    )
                },
                afterAnimatedContent = {
                    Column {
                        SwitchWithText(
                            text = stringResource(R.string.delete_all_contacts),
                            checked = uiState.deleteContactsPasscodeEnabled,
                            onCheckedChange = onDeleteAllContactsPasscodeToggle
                        )
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = ComposeAppTheme.colors.steel10,
                        )
                        RowWithArrow(
                            text = stringResource(R.string.login_logging_send_notification),
                            showAlert = !uiState.isPremiumActive,
                            onClick = onSendLoginNotificationClick
                        )
                    }
                },
                animatedVisible = uiState.logIntoDuressModeEnabled
            )

            VSpacer(16.dp)
            SectionUniversalLawrence {
                SwitchWithText(
                    text = stringResource(R.string.login_logging_delete_auth_data),
                    checked = uiState.deleteAllAuthDataOnDuressEnabled,
                    onCheckedChange = onDeleteAllAuthDataOnDuressToggle
                )
            }
            InfoText(
                text = stringResource(R.string.login_logging_delete_auth_data_description),
            )

            // Passcode
            VSpacer(28.dp)
            SectionUniversalLawrence {
                SwitchWithText(
                    text = stringResource(R.string.login_logging_passcode),
                    checked = uiState.passcodeEnabled,
                    onCheckedChange = onPasscodeToggle
                )
            }

            VFillSpacer(32.dp)

            // Auto Deleting Data Logs
            CellUniversalLawrenceSection(
                listOf {
                    HsSettingCell(
                        title = R.string.login_logging_auto_delete,
                        value = uiState.autoDeletePeriod.shortTitle.getString(),
                        onClick = { showPeriodSelector = true }
                    )
                }
            )

            VSpacer(16.dp)

            // Delete All Logs button
            CellUniversalLawrenceSection(
                listOf {
                    RowUniversal(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        onClick = if (uiState.deleteButtonEnabled) {
                            { showDeleteConfirmation = true }
                        } else null
                    ) {
                        val tintColor = if (uiState.deleteButtonEnabled) {
                            ComposeAppTheme.colors.lucian
                        } else {
                            ComposeAppTheme.colors.grey
                        }
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete_20),
                            contentDescription = null,
                            tint = tintColor
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        if (uiState.deleteButtonEnabled) {
                            body_lucian(text = stringResource(id = R.string.login_logging_delete_all))
                        } else {
                            body_grey(text = stringResource(id = R.string.login_logging_delete_all))
                        }
                    }
                }
            )
            VSpacer(32.dp)
        }
    }

    // Period Selector Dialog
    if (showPeriodSelector) {
        AlertGroup(
            title = R.string.login_logging_delete_period,
            select = Select(uiState.autoDeletePeriod, AutoDeletePeriod.entries),
            onSelect = { selected ->
                onAutoDeletePeriodChanged(selected)
                showPeriodSelector = false
            },
            onDismiss = { showPeriodSelector = false }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        DeleteLogsConfirmationDialog(
            onConfirm = {
                onDeleteAllLogs()
                showDeleteConfirmation = false
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }

    // Info Bottom Sheet
    if (showInfoSheet) {
        InfoBottomSheet(
            title = stringResource(R.string.login_logging_title),
            text = stringResource(R.string.login_logging_info_description),
            onDismiss = { showInfoSheet = false }
        )
    }

    // Camera Permission Denied Dialog
    if (showCameraPermissionDeniedDialog) {
        ConfirmationDialogBottomSheet(
            title = stringResource(R.string.camera_permission_denied_title),
            icon = R.drawable.icon_24_warning_2,
            warningTitle = null,
            warningText = stringResource(R.string.camera_permission_denied_message),
            actionButtonTitle = stringResource(R.string.button_open_settings),
            transparentButtonTitle = stringResource(R.string.Button_Cancel),
            onCloseClick = { showCameraPermissionDeniedDialog = false },
            onActionButtonClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
                showCameraPermissionDeniedDialog = false
            },
            onTransparentButtonClick = { showCameraPermissionDeniedDialog = false }
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoggingSettingsScreenPreview() {
    ComposeAppTheme {
        LoggingSettingsScreen(
            uiState = LoggingSettingsUiState(
                passcodeEnabled = false,
                deleteContactsPasscodeEnabled = false,
                logSuccessfulLoginsEnabled = true,
                selfieOnSuccessfulLoginEnabled = false,
                logUnsuccessfulLoginsEnabled = false,
                logIntoDuressModeEnabled = false,
                deleteAllAuthDataOnDuressEnabled = false,
                autoDeletePeriod = AutoDeletePeriod.YEAR,
                deleteButtonEnabled = true,
                noCameraPermissions = false,
                isPremiumActive = false
            ),
            onSelfieOnSuccessfulLoginToggle = {},
            onLogSuccessfulLoginsToggle = {},
            onLogUnsuccessfulLoginsToggle = {},
            onSelfieOnUnsuccessfulLoginToggle = {},
            onPasscodeToggle = {},
            onDeleteAllContactsPasscodeToggle = {},
            onLogIntoDuressModeToggle = {},
            onSelfieOnDuressLoginToggle = {},
            onSendLoginNotificationClick = {},
            onDeleteAllAuthDataOnDuressToggle = {},
            onAutoDeletePeriodChanged = {},
            onDeleteAllLogs = {},
            onClose = {}
        )
    }
}

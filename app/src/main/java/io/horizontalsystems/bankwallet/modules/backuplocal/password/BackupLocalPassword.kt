package io.horizontalsystems.bankwallet.modules.backuplocal.password

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellowWithSpinner
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputPassword
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper

@Composable
fun LocalBackupPasswordScreen(
    backupType: BackupType,
    onBackClick: () -> Unit,
    onFinish: () -> Unit
) {
    val viewModel = viewModel<BackupLocalPasswordViewModel>(factory = BackupLocalPasswordModule.Factory(backupType))

    val view = LocalView.current
    val context = LocalContext.current
    var hidePassphrase by remember { mutableStateOf(true) }
    val uiState = viewModel.uiState

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                uiState.backupJson?.let { backupJson ->
                    try {
                        outputStream.bufferedWriter().use { bw ->
                            bw.write(backupJson)
                            bw.flush()

                            HudHelper.showSuccessMessage(
                                contenView = view,
                                resId = R.string.LocalBackup_BackupSaved,
                                duration = SnackbarDuration.SHORT,
                                icon = R.drawable.ic_download_24,
                                iconTint = R.color.white
                            )

                            viewModel.backupFinished()
                        }
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

    if (uiState.backupJson != null) {
        App.pinComponent.keepUnlocked()
        backupLauncher.launch(viewModel.backupFileName)
    }

    if (uiState.closeScreen) {
        viewModel.closeScreenCalled()
        onFinish()
    }

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
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
                    VSpacer(32.dp)
                    TextImportantWarning(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(R.string.LocalBackup_DontForgetPasswordWarning)
                    )
                    VSpacer(32.dp)

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
}

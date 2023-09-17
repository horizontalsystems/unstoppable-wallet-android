package cash.p.terminal.modules.backuplocal.password

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.managers.PassphraseValidator
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.entities.DataState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BackupLocalPasswordViewModel(
    private val passphraseValidator: PassphraseValidator,
) : ViewModel() {

    private var passphrase = ""
    private var passphraseConfirmation = ""

    private var passphraseState: DataState.Error? = null
    private var passphraseConfirmState: DataState.Error? = null
    private var showButtonSpinner = false
    private var backupLocally = false
    private var closeScreen = false

    val backupJson: String
        get() = "repository.asJsonString"

    val backupFileName: String
        get() = "UW_Backup_${System.currentTimeMillis() / 1000}.json"

    var uiState by mutableStateOf(
        BackupLocalPasswordModule.UiState(
            passphraseState = null,
            passphraseConfirmState = null,
            showButtonSpinner = showButtonSpinner,
            backupLocally = backupLocally,
            closeScreen = closeScreen,
        )
    )
        private set

    fun onChangePassphrase(v: String) {
        if (passphraseValidator.validate(v)) {
            passphraseState = null
            passphrase = v
        } else {
            passphraseState = DataState.Error(
                Exception(
                    Translator.getString(R.string.CreateWallet_Error_PassphraseForbiddenSymbols)
                )
            )
        }
        syncState()
    }

    fun onChangePassphraseConfirmation(v: String) {
        passphraseConfirmState = null
        passphraseConfirmation = v
        syncState()
    }

    fun onSaveClick() {
        validatePassword()
        if (passphraseState == null && passphraseConfirmState == null) {
            showButtonSpinner = true
            backupLocally = true
            syncState()
        }
    }

    fun backupLocallyStarted() {
        backupLocally = false
        syncState()
    }

    fun backupFinished() {
        viewModelScope.launch {
            delay(2000) //Wait for showing Snackbar (SHORT duration ~ 1500ms)
            closeScreen = true
            syncState()
        }
    }

    fun closeScreenCalled() {
        closeScreen = false
        syncState()
    }

    private fun syncState() {
        uiState = BackupLocalPasswordModule.UiState(
            passphraseState = passphraseState,
            passphraseConfirmState = passphraseConfirmState,
            showButtonSpinner = showButtonSpinner,
            backupLocally = backupLocally,
            closeScreen = closeScreen,
        )
    }

    private fun validatePassword() {
        passphraseState = null
        passphraseConfirmState = null

        if (passphrase.isBlank()) {
            passphraseState = DataState.Error(
                Exception(Translator.getString(R.string.CreateWallet_Error_EmptyPassphrase))
            )
        } else if (passphrase.length < 8) {
            passphraseState = DataState.Error(
                Exception(Translator.getString(R.string.LocalBackup_ErrorPasswordLengthLessThan8))
            )
        } else if (passphrase != passphraseConfirmation) {
            passphraseConfirmState = DataState.Error(
                Exception(Translator.getString(R.string.CreateWallet_Error_InvalidConfirmation))
            )
        }

        syncState()
    }
}

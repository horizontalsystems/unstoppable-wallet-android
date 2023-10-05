package io.horizontalsystems.bankwallet.modules.backuplocal.password

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.PasswordError
import io.horizontalsystems.bankwallet.core.managers.PassphraseValidator
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.BackupProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class BackupType {
    class SingleWalletBackup(val accountId: String) : BackupType()
    class FullBackup(val accountIds: List<String>) : BackupType()
}

class BackupLocalPasswordViewModel(
    private val type: BackupType,
    private val passphraseValidator: PassphraseValidator,
    private val accountManager: IAccountManager,
    private val backupProvider: BackupProvider,
) : ViewModel() {

    private var passphrase = ""
    private var passphraseConfirmation = ""

    private var passphraseState: DataState.Error? = null
    private var passphraseConfirmState: DataState.Error? = null
    private var showButtonSpinner = false
    private var closeScreen = false
    private var error: String? = null

    private var backupJson: String? = null

    var backupFileName: String = "UW_Backup.json"
        private set

    var uiState by mutableStateOf(
        BackupLocalPasswordModule.UiState(
            passphraseState = null,
            passphraseConfirmState = null,
            showButtonSpinner = showButtonSpinner,
            backupJson = backupJson,
            closeScreen = closeScreen,
            error = error
        )
    )
        private set

    init {
        when (type) {
            is BackupType.SingleWalletBackup -> {
                val account = accountManager.account(type.accountId)
                if (account == null) {
                    error = "Account is NULL"

                } else {
                    val walletName = account.name.replace(" ", "_")
                    backupFileName = "UW_Backup_$walletName.json"
                }
            }

            is BackupType.FullBackup -> {
                backupFileName = "UW_App_Backup.json"
            }
        }

        syncState()
    }

    fun onChangePassphrase(v: String) {
        if (passphraseValidator.containsValidCharacters(v)) {
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
            syncState()
            saveAccount()
        }
    }

    fun backupFinished() {
        backupJson = null
        showButtonSpinner = false
        syncState()
        viewModelScope.launch {
            when (type) {
                is BackupType.SingleWalletBackup -> {
                    accountManager.account(type.accountId)?.let { account ->
                        if (!account.isFileBackedUp) {
                            accountManager.update(account.copy(isFileBackedUp = true))
                        }
                    }
                }

                is BackupType.FullBackup -> {
                    // FullBackup doesn't change account's backup state
                }
            }
            delay(1700) //Wait for showing Snackbar (SHORT duration ~ 1500ms)
            closeScreen = true
            syncState()
        }
    }

    fun closeScreenCalled() {
        closeScreen = false
        syncState()
    }

    fun accountErrorIsShown() {
        error = null
        syncState()
    }

    fun backupCanceled() {
        backupJson = null
        showButtonSpinner = false
        syncState()
    }

    private fun saveAccount() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                backupJson = when (type) {
                    is BackupType.FullBackup -> {
                        backupProvider.createFullBackup(
                            accountIds = type.accountIds,
                            passphrase = passphrase
                        )
                    }

                    is BackupType.SingleWalletBackup -> {
                        val account = accountManager.account(type.accountId) ?: throw Exception("Account is NULL")
                        backupProvider.createWalletBackup(
                            account = account.copy(isFileBackedUp = true),
                            passphrase = passphrase
                        )
                    }
                }
            } catch (t: Throwable) {
                error = t.message ?: t.javaClass.simpleName
            }

            withContext(Dispatchers.Main) {
                syncState()
            }
        }
    }

    private fun syncState() {
        uiState = BackupLocalPasswordModule.UiState(
            passphraseState = passphraseState,
            passphraseConfirmState = passphraseConfirmState,
            showButtonSpinner = showButtonSpinner,
            backupJson = backupJson,
            closeScreen = closeScreen,
            error = error
        )
    }

    private fun validatePassword() {
        passphraseState = null
        passphraseConfirmState = null

        try {
            passphraseValidator.validatePassword(passphrase)
        } catch (e: PasswordError) {
            passphraseState = DataState.Error(
                Exception(Translator.getString(R.string.LocalBackup_PasswordInvalid))
            )
            syncState()
            return
        }

        if (passphrase != passphraseConfirmation) {
            passphraseConfirmState = DataState.Error(
                Exception(Translator.getString(R.string.CreateWallet_Error_InvalidConfirmation))
            )
        }

        syncState()
    }
}

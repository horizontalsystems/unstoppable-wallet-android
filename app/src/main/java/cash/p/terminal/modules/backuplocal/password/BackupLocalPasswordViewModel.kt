package cash.p.terminal.modules.backuplocal.password

import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.IAccountManager
import cash.p.terminal.core.PasswordError
import cash.p.terminal.core.ViewModelUiState
import cash.p.terminal.core.managers.PassphraseValidator
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.stat
import cash.p.terminal.core.stats.statAccountType
import cash.p.terminal.entities.DataState
import cash.p.terminal.modules.backuplocal.fullbackup.BackupProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed class BackupType {
    class SingleWalletBackup(val accountId: String) : BackupType()
    class FullBackup(val accountIds: List<String>) : BackupType()
}

class BackupLocalPasswordViewModel(
    private val type: BackupType,
    private val passphraseValidator: PassphraseValidator,
    private val accountManager: IAccountManager,
    private val backupProvider: BackupProvider,
) : ViewModelUiState<BackupLocalPasswordModule.UiState>() {

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

    init {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")
        val currentDateTime = LocalDateTime.now().format(formatter)

        when (type) {
            is BackupType.SingleWalletBackup -> {
                val account = accountManager.account(type.accountId)
                if (account == null) {
                    error = "Account is NULL"

                } else {
                    val walletName = account.name.replace(" ", "_")
                    backupFileName = "UW_Backup_${walletName}_${currentDateTime}.json"
                }
            }

            is BackupType.FullBackup -> {
                backupFileName = "UW_App_Backup_${currentDateTime}.json"
            }
        }

        emitState()
    }

    override fun createState() = BackupLocalPasswordModule.UiState(
        passphraseState = passphraseState,
        passphraseConfirmState = passphraseConfirmState,
        showButtonSpinner = showButtonSpinner,
        backupJson = backupJson,
        closeScreen = closeScreen,
        error = error
    )

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
        emitState()
    }

    fun onChangePassphraseConfirmation(v: String) {
        passphraseConfirmState = null
        passphraseConfirmation = v
        emitState()
    }

    fun onSaveClick() {
        validatePassword()
        if (passphraseState == null && passphraseConfirmState == null) {
            showButtonSpinner = true
            emitState()
            saveAccount()
        }
    }

    fun backupFinished() {
        backupJson = null
        showButtonSpinner = false
        emitState()
        viewModelScope.launch {
            when (type) {
                is BackupType.SingleWalletBackup -> {
                    accountManager.account(type.accountId)?.let { account ->
                        if (!account.isFileBackedUp) {
                            accountManager.update(account.copy(isFileBackedUp = true))
                        }

                        stat(page = StatPage.ExportWalletToFiles, event = StatEvent.ExportWallet(account.type.statAccountType))
                    }
                }

                is BackupType.FullBackup -> {
                    // FullBackup doesn't change account's backup state

                    stat(page = StatPage.ExportFullToFiles, event = StatEvent.ExportFull)
                }
            }
            delay(1700) //Wait for showing Snackbar (SHORT duration ~ 1500ms)
            closeScreen = true
            emitState()
        }
    }

    fun closeScreenCalled() {
        closeScreen = false
        emitState()
    }

    fun accountErrorIsShown() {
        error = null
        emitState()
    }

    fun backupCanceled() {
        backupJson = null
        showButtonSpinner = false
        emitState()
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
                emitState()
            }
        }
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
            emitState()
            return
        }

        if (passphrase != passphraseConfirmation) {
            passphraseConfirmState = DataState.Error(
                Exception(Translator.getString(R.string.CreateWallet_Error_InvalidConfirmation))
            )
        }

        emitState()
    }
}

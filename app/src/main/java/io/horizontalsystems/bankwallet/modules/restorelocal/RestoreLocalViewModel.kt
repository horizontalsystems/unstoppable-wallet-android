package io.horizontalsystems.bankwallet.modules.restorelocal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.backuplocal.BackupLocalModule
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.BackupProvider
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.FullBackup
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.RestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RestoreLocalViewModel(
    private val backupJsonString: String?,
    private val accountFactory: IAccountFactory,
    private val backupProvider: BackupProvider,
    fileName: String?,
) : ViewModel() {

    private var passphrase = ""
    private var passphraseState: DataState.Error? = null
    private var showButtonSpinner = false
    private var walletBackup: BackupLocalModule.WalletBackup? = null
    private var fullBackup: FullBackup? = null
    private var parseError: Exception? = null
    private var accountType: AccountType? = null
    private var manualBackup = false
    private var restored = false

    val accountName by lazy {
        fileName?.let { name ->
            return@lazy name
                .replace(".json", "")
                .replace("UW_Backup_", "")
                .replace("_", " ")
        }
        accountFactory.getNextAccountName()
    }

    var uiState by mutableStateOf(
        RestoreLocalModule.UiState(
            passphraseState = null,
            showButtonSpinner = showButtonSpinner,
            parseError = parseError,
            accountType = accountType,
            manualBackup = manualBackup,
            restored = restored
        )
    )
        private set

    init {
        viewModelScope.launch {
            try {
                val gson = GsonBuilder()
                    .disableHtmlEscaping()
                    .enableComplexMapKeySerialization()
                    .create()

                fullBackup = try {
                    gson.fromJson(backupJsonString, FullBackup::class.java)
                } catch (ex: Exception) {
                    null
                }

                walletBackup = gson.fromJson(backupJsonString, BackupLocalModule.WalletBackup::class.java)
                manualBackup = walletBackup?.manualBackup ?: false
            } catch (e: Exception) {
                parseError = e
                syncState()
            }
        }
    }

    fun onChangePassphrase(v: String) {
        passphrase = v
        passphraseState = null
        syncState()
    }

    fun onImportClick() {
        when {
            fullBackup != null -> {
                fullBackup?.let { restoreFullBackup(it) }
            }

            walletBackup != null -> {
                walletBackup?.let { restoreSingleWallet(it, accountName) }
            }

            else -> {

            }
        }
    }

    private fun restoreFullBackup(fullBackup: FullBackup) {
        showButtonSpinner = true
        syncState()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                backupProvider.restore(fullBackup, passphrase)

                restored = true
            } catch (keyException: RestoreException.EncryptionKeyException) {
                parseError = keyException
            } catch (invalidPassword: RestoreException.InvalidPasswordException) {
                passphraseState = DataState.Error(Exception(Translator.getString(R.string.ImportBackupFile_Error_InvalidPassword)))
            }

            showButtonSpinner = false
            withContext(Dispatchers.Main) {
                syncState()
            }
        }
    }

    @Throws
    private fun restoreSingleWallet(backup: BackupLocalModule.WalletBackup, accountName: String) {
        showButtonSpinner = true
        syncState()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val type = backupProvider.accountType(backup, passphrase)
                if (type is AccountType.Cex) {
                    backupProvider.restoreCexAccount(type, accountName)

                    restored = true
                } else {
                    accountType = type
                }
            } catch (keyException: RestoreException.EncryptionKeyException) {
                parseError = keyException
            } catch (invalidPassword: RestoreException.InvalidPasswordException) {
                passphraseState = DataState.Error(Exception(Translator.getString(R.string.ImportBackupFile_Error_InvalidPassword)))
            } catch (e: IllegalStateException) {
                parseError = e
            }
            showButtonSpinner = false
            withContext(Dispatchers.Main) {
                syncState()
            }
        }
    }

    fun onSelectCoinsShown() {
        accountType = null
        syncState()
    }

    private fun syncState() {
        uiState = RestoreLocalModule.UiState(
            passphraseState = passphraseState,
            showButtonSpinner = showButtonSpinner,
            parseError = parseError,
            accountType = accountType,
            manualBackup = manualBackup,
            restored = restored
        )
    }

}

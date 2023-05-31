package cash.p.terminal.modules.restorelocal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import cash.p.terminal.R
import cash.p.terminal.core.IAccountFactory
import cash.p.terminal.core.managers.EncryptDecryptManager
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.entities.AccountType
import cash.p.terminal.entities.DataState
import cash.p.terminal.modules.backuplocal.BackupLocalModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RestoreLocalViewModel(
    private val backupJsonString: String?,
    accountFactory: IAccountFactory,
) : ViewModel() {

    private var passphrase = ""
    private var passphraseState: DataState.Error? = null
    private var showButtonSpinner = false
    private var walletBackup: BackupLocalModule.WalletBackup? = null
    private val encryptDecryptManager = EncryptDecryptManager()
    private var parseError: Exception? = null
    private var accountType: AccountType? = null

    val accountName by lazy {
        accountFactory.getNextAccountName()
    }

    var uiState by mutableStateOf(
        RestoreLocalModule.UiState(
            passphraseState = null,
            showButtonSpinner = showButtonSpinner,
            parseError = parseError,
            accountType = accountType,
        )
    )
        private set

    init {
        viewModelScope.launch {
            try {
                walletBackup = Gson().fromJson(backupJsonString, BackupLocalModule.WalletBackup::class.java)
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
        val backup = walletBackup ?: return
        showButtonSpinner = true
        syncState()
        viewModelScope.launch(Dispatchers.IO) {
            val kdfParams = backup.crypto.kdfparams
            val key = EncryptDecryptManager.getKey(passphrase, kdfParams) ?: return@launch
            if (EncryptDecryptManager.passwordIsCorrect(backup.crypto.mac, backup.crypto.ciphertext, key)) {
                val decrypted = encryptDecryptManager.decrypt(backup.crypto.ciphertext, key, backup.crypto.cipherparams.iv)
                try {
                    accountType = BackupLocalModule.getAccountTypeFromString(backup.type, decrypted)
                } catch (e: IllegalStateException) {
                    parseError = e
                }
            } else {
                passphraseState = DataState.Error(Exception(Translator.getString(R.string.ImportBackupFile_Error_InvalidPassword)))
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
        )
    }

}

package cash.p.terminal.modules.backuplocal.password

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import cash.p.terminal.R
import cash.p.terminal.core.IAccountManager
import cash.p.terminal.core.managers.EncryptDecryptManager
import cash.p.terminal.core.managers.PassphraseValidator
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.entities.Account
import cash.p.terminal.entities.DataState
import cash.p.terminal.modules.backuplocal.BackupLocalModule
import cash.p.terminal.modules.backuplocal.BackupLocalModule.WalletBackup
import io.horizontalsystems.core.toHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class BackupLocalPasswordViewModel(
    private val passphraseValidator: PassphraseValidator,
    private val accountManager: IAccountManager,
    accountId: String?,
) : ViewModel() {

    private var account: Account? = null
    private var passphrase = ""
    private var passphraseConfirmation = ""

    private var passphraseState: DataState.Error? = null
    private var passphraseConfirmState: DataState.Error? = null
    private var showButtonSpinner = false
    private var closeScreen = false
    private var showAccountIsNullError = false
    private val encryptDecryptManager = EncryptDecryptManager()
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
            showAccountIsNullError = showAccountIsNullError
        )
    )
        private set

    init {
        val account = accountId?.let { accountManager.account(it) }
        if (account == null) {
            showAccountIsNullError = true
            syncState()
        } else {
            this.account = account
            val walletName = account.name.replace(" ", "_")
            backupFileName = "UW_Backup_$walletName.json"
        }
    }

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
            syncState()
            saveAccount()
        }
    }

    fun backupFinished() {
        backupJson = null
        showButtonSpinner = false
        syncState()
        viewModelScope.launch {
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
        showAccountIsNullError = false
        syncState()
    }

    private fun saveAccount() {
        val accountType = account?.type ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val kdfParams = BackupLocalModule.kdfDefault
            val secretText = BackupLocalModule.getStringForEncryption(accountType)
            val id = getId(secretText)
            val key = EncryptDecryptManager.getKey(passphrase, kdfParams) ?: return@launch

            val iv = EncryptDecryptManager.generateRandomBytes(16).toHexString()
            val encrypted = encryptDecryptManager.encrypt(secretText, key, iv)
            val mac = EncryptDecryptManager.generateMac(key, encrypted.toByteArray())

            val crypto = BackupLocalModule.BackupCrypto(
                cipher = "aes-128-ctr",
                cipherparams = BackupLocalModule.CipherParams(iv),
                ciphertext = encrypted,
                kdf = "scrypt",
                kdfparams = kdfParams,
                mac = mac.toHexString()
            )

            val backup = WalletBackup(
                crypto = crypto,
                id = id,
                type = BackupLocalModule.getAccountTypeString(accountType),
                timestamp = System.currentTimeMillis() / 1000,
                version = 1
            )

            val gson = GsonBuilder()
                .disableHtmlEscaping()
                .create()
            backupJson = gson.toJson(backup)
            withContext(Dispatchers.Main) {
                syncState()
            }
        }
    }

    private fun getId(words: String): String {
        val md = MessageDigest.getInstance("SHA-512")
        val digest = md.digest(words.toByteArray())
        return digest.toHexString()
    }

    private fun syncState() {
        uiState = BackupLocalPasswordModule.UiState(
            passphraseState = passphraseState,
            passphraseConfirmState = passphraseConfirmState,
            showButtonSpinner = showButtonSpinner,
            backupJson = backupJson,
            closeScreen = closeScreen,
            showAccountIsNullError = showAccountIsNullError
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

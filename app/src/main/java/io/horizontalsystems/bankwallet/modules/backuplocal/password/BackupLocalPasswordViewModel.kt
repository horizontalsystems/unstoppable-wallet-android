package io.horizontalsystems.bankwallet.modules.backuplocal.password

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.EncryptDecryptManager
import io.horizontalsystems.bankwallet.core.managers.PassphraseValidator
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.backuplocal.BackupLocalModule
import io.horizontalsystems.bankwallet.modules.backuplocal.BackupLocalModule.WalletBackup
import io.horizontalsystems.core.toHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest

class BackupLocalPasswordViewModel(
    private val passphraseValidator: PassphraseValidator,
    private val accountManager: IAccountManager,
    accountId: String?,
) : ViewModel() {

    private lateinit var account: Account
    private var passphrase = ""
    private var passphraseConfirmation = ""

    private var passphraseState: DataState.Error? = null
    private var passphraseConfirmState: DataState.Error? = null
    private var showButtonSpinner = false
    private var backupLocally = false
    private var closeScreen = false
    private var showAccountIsNullError = false
    private val encryptDecryptManager = EncryptDecryptManager()

    var backupJson: String? = null
        private set

    var backupFileName: String = "UW_Backup.json"
        private set

    var uiState by mutableStateOf(
        BackupLocalPasswordModule.UiState(
            passphraseState = null,
            passphraseConfirmState = null,
            showButtonSpinner = showButtonSpinner,
            backupLocally = backupLocally,
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

    fun backupLocallyStarted() {
        backupLocally = false
        syncState()
    }

    fun backupFinished() {
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
        viewModelScope.launch(Dispatchers.IO) {
            val kdfParams = BackupLocalModule.kdfDefault
            val secretText = BackupLocalModule.getStringForEncryption(account.type)
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
                type = BackupLocalModule.getAccountTypeString(account.type),
                version = 1
            )

            val gson = GsonBuilder()
                .disableHtmlEscaping()
                .create()
            backupJson = gson.toJson(backup)
            backupLocally = true
            syncState()
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
            backupLocally = backupLocally,
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

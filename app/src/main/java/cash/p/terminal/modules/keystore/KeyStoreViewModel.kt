package cash.p.terminal.modules.keystore

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cash.p.terminal.core.ILocalStorage
import io.horizontalsystems.core.IKeyStoreManager
import org.koin.java.KoinJavaComponent.inject

class KeyStoreViewModel(
    private val keyStoreManager: IKeyStoreManager,
    mode: KeyStoreModule.ModeType
) : ViewModel() {

    private val localStorage: ILocalStorage by inject(ILocalStorage::class.java)

    var showSystemLockWarning by mutableStateOf(false)
        private set

    var showBiometricPrompt by mutableStateOf(false)
        private set

    var showInvalidKeyWarning by mutableStateOf(false)
        private set

    var openMainModule by mutableStateOf(false)
        private set

    var closeApp by mutableStateOf(false)
        private set


    var showTermsDialog by mutableStateOf(false)
        private set

    var isSystemPinRequired  by mutableStateOf(localStorage.isSystemPinRequired)
        private set

    init {
        when (mode) {
            KeyStoreModule.ModeType.NoSystemLock -> {
                showSystemLockWarning = true
                keyStoreManager.resetApp("NoSystemLock")
            }

            KeyStoreModule.ModeType.InvalidKey -> {
                showInvalidKeyWarning = true
                keyStoreManager.resetApp("InvalidKey")
            }

            KeyStoreModule.ModeType.UserAuthentication -> {
                showBiometricPrompt = true
            }
        }
    }

    fun onCloseInvalidKeyWarning() {
        keyStoreManager.removeKey()
        showInvalidKeyWarning = false
        openMainModule = true
    }

    fun onAuthenticationCanceled() {
        showBiometricPrompt = false
        closeApp = true
    }

    fun onAuthenticationSuccess() {
        showBiometricPrompt = false
        openMainModule = true
    }

    fun openMainModuleCalled() {
        openMainModule = false
    }

    fun closeAppCalled() {
        closeApp = false
    }

    fun onShowTermsDialog() {
        showTermsDialog = true
    }

    fun onCloseTermsDialog() {
        showTermsDialog = false
    }

    fun onTermsAccepted() {
        showTermsDialog = false

        isSystemPinRequired = false
        localStorage.isSystemPinRequired = false
    }
}

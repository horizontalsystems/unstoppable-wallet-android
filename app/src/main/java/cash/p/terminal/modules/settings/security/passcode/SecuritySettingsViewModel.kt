package cash.p.terminal.modules.settings.security.passcode

import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.managers.TransactionHiddenManager
import cash.p.terminal.tangem.domain.sdk.CardSdkConfigRepository
import cash.p.terminal.wallet.managers.ITransactionHiddenManager
import cash.p.terminal.wallet.managers.TransactionDisplayLevel
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.ViewModelUiState
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import org.koin.java.KoinJavaComponent.inject

class SecuritySettingsViewModel(
    private val cardSdkConfigRepository: CardSdkConfigRepository
) : ViewModelUiState<SecuritySettingsUiState>() {

    private val systemInfoManager = CoreApp.systemInfoManager
    private val pinComponent = CoreApp.pinComponent
    private val balanceHiddenManager = App.balanceHiddenManager
    private val localStorage = App.localStorage

    val biometricSettingsVisible = systemInfoManager.biometricAuthSupported

    private val transactionHiddenManager: TransactionHiddenManager by inject(
        ITransactionHiddenManager::class.java
    )

    private var pinEnabled = pinComponent.isPinSet
    private var duressPinEnabled = pinComponent.isDuressPinSet()
    private var balanceAutoHideEnabled = balanceHiddenManager.balanceAutoHidden

    init {
        viewModelScope.launch {
            pinComponent.pinSetFlowable.asFlow().collect {
                pinEnabled = pinComponent.isPinSet
                duressPinEnabled = pinComponent.isDuressPinSet()
                emitState()
            }
        }
    }

    override fun createState() = SecuritySettingsUiState(
        pinEnabled = pinEnabled,
        biometricsEnabled = pinComponent.isBiometricAuthEnabled,
        duressPinEnabled = duressPinEnabled,
        balanceAutoHideEnabled = balanceAutoHideEnabled,
        transactionAutoHideEnabled = transactionHiddenManager.transactionHiddenFlow.value.transactionHidden,
        displayLevel = transactionHiddenManager.transactionHiddenFlow.value.transactionDisplayLevel,
        transactionAutoHideSeparatePinExists =
            transactionHiddenManager.transactionHiddenFlow.value.transactionAutoHidePinExists,
        autoLockIntervalName = localStorage.autoLockInterval.title,
        transferPasscodeEnabled = localStorage.transferPasscodeEnabled,
        isSaveAccessCodeForHardwareWalletEnabled = cardSdkConfigRepository.isBiometricsRequestPolicy,
        isSystemPinRequired = localStorage.isSystemPinRequired,
        isDeviceSecure = systemInfoManager.isDeviceSecure,
        isSystemPinRequiredEnabled = !localStorage.isSystemPinRequired
    )

    fun enableBiometrics() {
        pinComponent.isBiometricAuthEnabled = true
        emitState()
    }

    fun onTransferPasscodeEnabledChange(enabled: Boolean) {
        localStorage.transferPasscodeEnabled = enabled
        emitState()
    }

    fun onTransactionAutoHideEnabledChange(enabled: Boolean, emitState: Boolean = true) {
        transactionHiddenManager.setTransactionHideEnabled(enabled)
        transactionHiddenManager.clearSeparatePin()
        transactionHiddenManager.setTransactionDisplayLevel(TransactionDisplayLevel.NOTHING)
        if (emitState) {
            emitState()
        }
    }

    fun onDisableTransactionAutoHidePin() {
        transactionHiddenManager.clearSeparatePin()
        if (balanceHiddenManager.balanceAutoHidden) {
            balanceHiddenManager.setBalanceAutoHidden(false)
        }
        if (!pinComponent.isPinSet) {
            transactionHiddenManager.setTransactionHideEnabled(false)
        }
        emitState()
    }

    fun disableBiometrics() {
        pinComponent.isBiometricAuthEnabled = false
        emitState()
    }

    fun disablePin() {
        pinComponent.disablePin()
        if (localStorage.transactionHideSecretPin == null) {
            onTransactionAutoHideEnabledChange(enabled = false, emitState = false)
        }
        pinComponent.isBiometricAuthEnabled = false
        localStorage.transferPasscodeEnabled = false
        emitState()
    }

    fun disableDuressPin() {
        pinComponent.disableDuressPin()
        emitState()
    }

    fun onSetBalanceAutoHidden(enabled: Boolean) {
        balanceAutoHideEnabled = enabled
        emitState()
        balanceHiddenManager.setBalanceAutoHidden(enabled)
    }

    fun update() {
        emitState()
    }

    fun enableSaveAccessCodeForHardwareWallet(enabled: Boolean) {
        cardSdkConfigRepository.isBiometricsRequestPolicy = enabled
        emitState()
    }

    fun onSystemPinRequiredChange(enabled: Boolean) {
        localStorage.isSystemPinRequired = enabled
        emitState()
    }
}

data class SecuritySettingsUiState(
    val pinEnabled: Boolean,
    val biometricsEnabled: Boolean,
    val duressPinEnabled: Boolean,
    val balanceAutoHideEnabled: Boolean,
    val transactionAutoHideEnabled: Boolean,
    val displayLevel: TransactionDisplayLevel,
    val transactionAutoHideSeparatePinExists: Boolean,
    val transferPasscodeEnabled: Boolean,
    val autoLockIntervalName: Int,
    val isSaveAccessCodeForHardwareWalletEnabled: Boolean,
    val isSystemPinRequired: Boolean,
    val isDeviceSecure: Boolean,
    val isSystemPinRequiredEnabled: Boolean,
)
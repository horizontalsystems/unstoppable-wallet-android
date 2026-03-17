package io.horizontalsystems.bankwallet.modules.settings.security.passcode

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.BalanceHiddenManager
import io.horizontalsystems.bankwallet.core.managers.PaidActionSettingsManager
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.launch

class SecuritySettingsViewModel(
    private val systemInfoManager: ISystemInfoManager,
    private val pinComponent: IPinComponent,
    private val balanceHiddenManager: BalanceHiddenManager,
    private val localStorage: ILocalStorage,
    private val paidActionSettingsManager: PaidActionSettingsManager
) : ViewModelUiState<SecuritySettingsUiState>() {
    val biometricSettingsVisible = systemInfoManager.biometricAuthSupported

    private var pinEnabled = pinComponent.isPinSet
    private var duressPinEnabled = pinComponent.isDuressPinSet()
    private var balanceAutoHideEnabled = balanceHiddenManager.balanceAutoHidden
    private var defenseSystemActions = listOf<DefenseSystemAction>()

    init {
        viewModelScope.launch {
            pinComponent.pinSetFlow.collect {
                pinEnabled = pinComponent.isPinSet
                duressPinEnabled = pinComponent.isDuressPinSet()
                emitState()
            }
        }

        viewModelScope.launch {
            paidActionSettingsManager.disabledActionsFlow.collect {
                refreshDefenseSystemActions()
            }
        }

        viewModelScope.launch {
            UserSubscriptionManager.activeSubscriptionStateFlow.collect {
                refreshDefenseSystemActions()
            }
        }
    }

    private fun refreshDefenseSystemActions() {
        defenseSystemActions = paidActionSettingsManager.toggleableActions.map {
            DefenseSystemAction(it, paidActionSettingsManager.isActionActive(it))
        }

        emitState()
    }

    override fun createState() = SecuritySettingsUiState(
        pinEnabled = pinEnabled,
        biometricsEnabled = pinComponent.isBiometricAuthEnabled,
        duressPinEnabled = duressPinEnabled,
        balanceAutoHideEnabled = balanceAutoHideEnabled,
        autoLockIntervalName = localStorage.autoLockInterval.title,
        defenseSystemActions = defenseSystemActions,
    )

    fun enableBiometrics() {
        pinComponent.isBiometricAuthEnabled = true
        emitState()
    }

    fun disableBiometrics() {
        pinComponent.isBiometricAuthEnabled = false
        emitState()
    }

    fun disablePin() {
        pinComponent.disablePin()
        pinComponent.isBiometricAuthEnabled = false
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

    fun setActionEnabled(action: IPaidAction, enabled: Boolean) {
        paidActionSettingsManager.setActionEnabled(action, enabled)
    }
}

data class SecuritySettingsUiState(
    val pinEnabled: Boolean,
    val biometricsEnabled: Boolean,
    val duressPinEnabled: Boolean,
    val balanceAutoHideEnabled: Boolean,
    val autoLockIntervalName: Int,
    val defenseSystemActions: List<DefenseSystemAction>
)

data class DefenseSystemAction(val action: IPaidAction, val enabled: Boolean)
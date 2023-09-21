package cash.p.terminal.modules.settings.security.passcode

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.managers.UserManager
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

class SecurityPasscodeSettingsViewModel(
    private val systemInfoManager: ISystemInfoManager,
    private val pinComponent: IPinComponent,
    private val userManager: UserManager
) : ViewModel() {

    init {
        viewModelScope.launch {
            pinComponent.pinSetFlowable.asFlow().collect {
                syncPinSet(pinComponent.isPinSet)
            }
        }
    }

    var pinEnabled by mutableStateOf(pinComponent.isPinSet)
        private set

    var biometricSettingsVisible by mutableStateOf(pinComponent.isPinSet && systemInfoManager.biometricAuthSupported)
        private set

    var biometricEnabled by mutableStateOf(pinComponent.isBiometricAuthEnabled)
        private set

    fun setBiometricAuth(enabled: Boolean) {
        pinComponent.isBiometricAuthEnabled = enabled
        biometricEnabled = enabled
    }

    fun disablePin() {
        pinComponent.clear(userManager.getUserLevel())
        biometricEnabled = false
    }

    private fun syncPinSet(pinSet: Boolean) {
        pinEnabled = pinSet
        biometricSettingsVisible = pinSet && systemInfoManager.biometricAuthSupported
    }

}

package io.horizontalsystems.bankwallet.modules.settings.security.tor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.ITorManager
import io.horizontalsystems.core.IPinComponent
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await

class SecurityTorSettingsViewModel(
    private val torManager: ITorManager,
    private val pinComponent: IPinComponent,
) : ViewModel() {

    private val logger = AppLogger("SecurityTorSettingsViewModel")

    var torConnectionStatus by mutableStateOf(TorStatus.Closed)
        private set

    var torCheckEnabled by mutableStateOf(torManager.isTorEnabled)
        private set

    var showRestartAlert by mutableStateOf(false)
        private set

    var restartApp by mutableStateOf(false)
        private set


    init {
        torManager.torStatusFlow
            .onEach { connectionStatus ->
                torConnectionStatus = connectionStatus
            }.launchIn(viewModelScope)
    }

    fun setTorEnabledWithChecks(enabled: Boolean) {
        torCheckEnabled = enabled
        showRestartAlert = true
    }

    fun setTorEnabled() {
        if (torCheckEnabled) {
            torManager.setTorAsEnabled()
            restartApp = true
        } else {
            torManager.setTorAsDisabled()
            viewModelScope.launch {
                try {
                    torManager.stop().await()
                    pinComponent.updateLastExitDateBeforeRestart()
                    restartApp = true
                } catch (e: Throwable) {
                    logger.warning("Tor exception", e)
                }
            }
        }
    }

    fun restartAppAlertShown() {
        showRestartAlert = false
    }

    fun appRestarted() {
        restartApp = false
    }

    fun resetSwitch() {
        torCheckEnabled = torManager.isTorEnabled
    }

}

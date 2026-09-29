package io.horizontalsystems.walletkit.modules.settings.privacy.tor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.IPinComponent
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.AppLogger
import io.horizontalsystems.walletkit.core.ITorManager
import kotlinx.coroutines.launch

class SecurityTorSettingsViewModel(
    private val torManager: ITorManager,
    private val pinComponent: IPinComponent,
) : ViewModel() {

    private val logger = AppLogger("SecurityTorSettingsViewModel")

    var torCheckEnabled by mutableStateOf(torManager.isTorEnabled)
        private set

    var showRestartAlert by mutableStateOf(false)
        private set

    var restartApp by mutableStateOf(false)
        private set

    fun setTorEnabledWithChecks(enabled: Boolean) {
        torCheckEnabled = enabled
        showRestartAlert = true
    }

    fun setTorEnabled() {
        if (torCheckEnabled) {
            torManager.setTorAsEnabled()
            App.pinComponent.keepUnlocked()
            restartApp = true
        } else {
            torManager.setTorAsDisabled()
            viewModelScope.launch {
                try {
                    torManager.stop()
                } catch (e: Throwable) {
                    logger.warning("Tor exception", e)
                }
                // The preference is already off, so the app restarts into that state even when
                // stopping Tor fails
                pinComponent.updateLastExitDateBeforeRestart()
                App.pinComponent.keepUnlocked()
                restartApp = true
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

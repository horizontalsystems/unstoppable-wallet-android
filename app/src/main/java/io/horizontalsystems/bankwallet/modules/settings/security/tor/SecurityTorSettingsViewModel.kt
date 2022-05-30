package io.horizontalsystems.bankwallet.modules.settings.security.tor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable

class SecurityTorSettingsViewModel(
    private val service: SecurityTorSettingsService
) : ViewModel() {

    private var disposables: CompositeDisposable = CompositeDisposable()

    var torConnectionStatus by mutableStateOf(TorStatus.Closed)
        private set

    var torCheckEnabled by mutableStateOf(service.torEnabled)
        private set

    var showTorNotificationNotEnabledAlert by mutableStateOf(false)
        private set

    var showRestartAlert by mutableStateOf(false)
        private set

    var restartApp by mutableStateOf(false)
        private set


    init {
        service.torConnectionStatusObservable
            .subscribe {
                onTorConnectionStatusUpdated(it)
            }.let {
                disposables.add(it)
            }

        service.restartAppObservable
            .subscribe {
                restartApp = true
            }.let {
                disposables.add(it)
            }

        service.start()
    }

    override fun onCleared() {
        disposables.clear()
        service.stop()
    }

    fun setTorEnabledWithChecks(enabled: Boolean) {
        if (enabled && !service.isTorNotificationEnabled) {
            showTorNotificationNotEnabledAlert = true
        } else {
            torCheckEnabled = enabled
            showRestartAlert = true
        }
    }

    fun setTorEnabled(enabled: Boolean) {
        if (enabled) {
            service.enableTor()
        } else {
            service.disableTor()
        }
    }

    fun torNotificationNotEnabledAlertShown() {
        showTorNotificationNotEnabledAlert = false
    }

    fun restartAppAlertShown() {
        showRestartAlert = false
    }

    fun appRestarted() {
        restartApp = false
    }

    private fun onTorConnectionStatusUpdated(connectionStatus: TorStatus) {
        torConnectionStatus = connectionStatus
        torCheckEnabled = service.torEnabled
    }

    fun resetSwitch() {
        torCheckEnabled = service.torEnabled
    }

}

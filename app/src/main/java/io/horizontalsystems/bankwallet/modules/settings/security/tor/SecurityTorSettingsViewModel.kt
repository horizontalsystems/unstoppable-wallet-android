package io.horizontalsystems.bankwallet.modules.settings.security.tor

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ITorManager
import io.horizontalsystems.core.IPinComponent
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SecurityTorSettingsViewModel(
    private val torManager: ITorManager,
    private val pinComponent: IPinComponent,
) : ViewModel() {

    private var disposables: CompositeDisposable = CompositeDisposable()

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

    override fun onCleared() {
        disposables.clear()
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
            torManager.stop()
                .subscribe({
                    pinComponent.updateLastExitDateBeforeRestart()
                    restartApp = true
                }, {
                    Log.e("SecurityTorSettingsViewModel", "stopTor", it)
                })
                .let {
                    disposables.add(it)
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

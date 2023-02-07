package cash.p.terminal.modules.tor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.ITorManager
import cash.p.terminal.modules.settings.security.tor.TorStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class TorConnectionViewModel(
    private val torManager: ITorManager
) : ViewModel() {

    var closeView by mutableStateOf(false)
    var restartApp by mutableStateOf(false)
    var torStatus by mutableStateOf<TorStatus?>(null)

    init {
        viewModelScope.launch {
            torManager.torStatusFlow.collect { connectionStatus ->
                if (connectionStatus == TorStatus.Connected) {
                    closeView = true
                } else {
                    torStatus = connectionStatus
                }
            }
        }
    }

    fun restartTor() {
        torManager.start()
    }

    fun stopTor() {
        torManager.setTorAsDisabled()
        viewModelScope.launch {
            torManager.stop().toObservable().asFlow().collect {
                restartApp = true
            }
        }
    }

    fun viewClosed() {
        closeView = false
    }

    fun restartAppCalled() {
        restartApp = false
    }
}

package io.horizontalsystems.bankwallet.modules.tor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ITorManager
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.modules.settings.security.tor.TorStatus
import io.horizontalsystems.bankwallet.modules.tor.TorConnectionModule.TorViewState
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class TorConnectionViewModel(
    private val torManager: ITorManager,
    private val connectivityManager: ConnectivityManager
) : ViewModel() {

    var torViewState by mutableStateOf(getState())

    private var stateText = R.string.TorPage_Connecting
    private var showRetryButton = false
    private var showNoNetworkConnectionError = false
    private var torIsActive = false

    init {
        torManager.torStatusFlow
            .onEach { connectionStatus ->
                updateTorViewState(connectionStatus)
            }.launchIn(viewModelScope)
    }

    fun restartTor() {
        if (!connectivityManager.isConnected){
            showNoNetworkConnectionError = true
            emitState()
            return
        }
        torManager.start()
    }

    fun networkErrorShown(){
        showNoNetworkConnectionError = false
        emitState()
    }

    private fun updateTorViewState(torStatus: TorStatus) {
        torIsActive = torStatus == TorStatus.Connected
        showRetryButton = torStatus == TorStatus.Failed

        if (torStatus != TorStatus.Connected && !connectivityManager.isConnected) {
            showNoNetworkConnectionError = true
        }

        stateText = when(torStatus) {
            TorStatus.Connected -> R.string.Tor_TorIsActive
            TorStatus.Failed -> R.string.TorPage_Failed
            else -> R.string.TorPage_Connecting
        }

        emitState()
    }

    private fun getState(): TorViewState {
        return TorViewState(
            stateText = stateText,
            showRetryButton = showRetryButton,
            torIsActive = torIsActive,
            showNetworkConnectionError = showNoNetworkConnectionError
        )
    }

    private fun emitState(){
        torViewState = getState()
    }
}

package io.horizontalsystems.bankwallet.modules.walletconnect.session.v2

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WalletConnectMainViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WalletConnectSessionModule
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Request
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2PingService
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class WC2SessionViewModel(private val service: WC2SessionService) : ViewModel() {

    private val TAG = "WC2SessionViewModel"

    val peerMetaLiveData = MutableLiveData<WalletConnectSessionModule.PeerMetaItem?>()
    val buttonStatesLiveData = MutableLiveData<WCSessionButtonStates>()
    val hintLiveData = MutableLiveData<Int?>()
    val errorLiveData = MutableLiveData<String?>()
    val statusLiveData = MutableLiveData<WalletConnectMainViewModel.Status?>()
    val closeLiveEvent = SingleLiveEvent<Unit>()
    val openRequestLiveEvent = SingleLiveEvent<WC1Request>()

    private val disposables = CompositeDisposable()

    var invalidUrlError by mutableStateOf(false)
        private set

    var closeEnabled by mutableStateOf(false)
        private set

    var connecting by mutableStateOf(false)
        private set

    init {
        service.connectionStateObservable
            .subscribe {
                Log.e(TAG, "sync from connection change: $it")
                sync(connectionState = it)
            }
            .let {
                disposables.add(it)
            }

        service.stateObservable
            .subscribe {
                Log.e(TAG, "sync from state change: $it")
                sync(sessionState = it)
            }
            .let {
                disposables.add(it)
            }

        service.start()
    }

    override fun onCleared() {
        service.stop()
    }

    private fun sync(
        sessionState: WC2SessionService.State? = null,
        connectionState: WC2PingService.State? = null
    ) {
        val state = sessionState ?: service.state
        val connection = connectionState ?: service.connectionState

        if (state == WC2SessionService.State.Killed) {
            closeLiveEvent.postValue(Unit)
            return
        }

        when (state) {
            WC2SessionService.State.WaitingForApproveSession,
            WC2SessionService.State.Ready -> {
                peerMetaLiveData.postValue(service.appMetaItem)
            }
        }

        connecting = connection == WC2PingService.State.Connecting
        closeEnabled = state == WC2SessionService.State.Ready

        val cancelBtnState = getCancelButtonState(state)
        val connectBtnState = getConnectButtonState(state, connection)
        val disconnectBtnState = getDisconnectButtonState(state, connection)
        val reconnectBtnState = getReconnectButtonState(connection)

        buttonStatesLiveData.postValue(
            WCSessionButtonStates(
                connectBtnState,
                disconnectBtnState,
                cancelBtnState,
                reconnectBtnState
            )
        )

        statusLiveData.postValue(getStatus(connection))

        hintLiveData.postValue(getHint(connection, state))
    }

    private fun getHint(connection: WC2PingService.State, state: WC2SessionService.State): Int? =
        when {
            connection is WC2PingService.State.Disconnected -> R.string.WalletConnect_Reconnect_Hint
            connection != WC2PingService.State.Connected -> null
            state == WC2SessionService.State.WaitingForApproveSession -> R.string.WalletConnect_Approve_Hint
            state == WC2SessionService.State.Ready -> R.string.WalletConnect_Ready_Hint
            else -> null
        }

    fun cancel() {
        service.reject()
    }

    fun connect() {
        service.approve()
    }

    fun disconnect() {
        service.disconnect()
    }

    fun reconnect() {
        service.reconnect()
    }

    private fun getStatus(connectionState: WC2PingService.State): WalletConnectMainViewModel.Status? {
        return when (connectionState) {
            WC2PingService.State.Connecting -> WalletConnectMainViewModel.Status.CONNECTING
            WC2PingService.State.Connected -> WalletConnectMainViewModel.Status.ONLINE
            is WC2PingService.State.Disconnected -> WalletConnectMainViewModel.Status.OFFLINE
        }
    }

    private fun getCancelButtonState(state: WC2SessionService.State): WCButtonState {
        return if (state != WC2SessionService.State.Ready) {
            WCButtonState.Enabled
        } else {
            WCButtonState.Hidden
        }
    }

    private fun getConnectButtonState(
        state: WC2SessionService.State,
        connectionState: WC2PingService.State
    ): WCButtonState {
        return when {
            state == WC2SessionService.State.WaitingForApproveSession &&
                    connectionState == WC2PingService.State.Connected -> WCButtonState.Enabled
            else -> WCButtonState.Hidden
        }
    }

    private fun getDisconnectButtonState(
        state: WC2SessionService.State,
        connectionState: WC2PingService.State
    ): WCButtonState {
        return when {
            state == WC2SessionService.State.Ready &&
                    connectionState == WC2PingService.State.Connected -> WCButtonState.Enabled
            else -> WCButtonState.Hidden
        }
    }

    private fun getReconnectButtonState(connectionState: WC2PingService.State): WCButtonState {
        return when (connectionState) {
            is WC2PingService.State.Disconnected -> WCButtonState.Enabled
            is WC2PingService.State.Connecting -> WCButtonState.Disabled
            else -> WCButtonState.Hidden
        }
    }
}

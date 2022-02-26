package io.horizontalsystems.bankwallet.modules.walletconnect.session.v2

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.walletconnect.RequestType
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WCSessionModule
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WCSessionViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2PingService
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class WC2SessionViewModel(private val service: WC2SessionService) : ViewModel() {

    private val TAG = "WC2SessionViewModel"

    val closeLiveEvent = SingleLiveEvent<Unit>()
    val openRequestLiveEvent = SingleLiveEvent<Pair<Long, RequestType>>()

    private val disposables = CompositeDisposable()

    var peerMeta by mutableStateOf<WCSessionModule.PeerMetaItem?>(null)
        private set

    var invalidUrlError by mutableStateOf(false)
        private set

    var closeEnabled by mutableStateOf(false)
        private set

    var connecting by mutableStateOf(false)
        private set

    var buttonStates by mutableStateOf<WCSessionButtonStates?>(null)
        private set

    var hint by mutableStateOf<Int?>(null)
        private set

    var showError by mutableStateOf<String?>(null)
        private set

    var status by mutableStateOf<WCSessionViewModel.Status?>(null)
        private set

    init {
        service.connectionStateObservable
            .subscribeIO {
                Log.e(TAG, "sync from connection change: $it")
                sync(connectionState = it)
            }
            .let {
                disposables.add(it)
            }

        service.stateObservable
            .subscribeIO {
                Log.e(TAG, "sync from state change: $it")
                sync(sessionState = it)
            }
            .let {
                disposables.add(it)
            }

        service.pendingRequestObservable
            .subscribeIO{ sessionRequest ->
                val requestType = RequestType.fromString(sessionRequest.request.method)
                requestType?.let {
                    openRequestLiveEvent.postValue(Pair(sessionRequest.request.id, it))
                }
                Log.e(TAG, "request: ${sessionRequest.request.method}")
            }.let {
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

        peerMeta = service.appMetaItem
        connecting = connection == WC2PingService.State.Connecting
        closeEnabled = state == WC2SessionService.State.Ready
        status = getStatus(connection)
        hint = getHint(connection, state)

        setButtons(state, connection)
        setError(state)
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

    private fun getStatus(connectionState: WC2PingService.State): WCSessionViewModel.Status? {
        return when (connectionState) {
            WC2PingService.State.Connecting -> WCSessionViewModel.Status.CONNECTING
            WC2PingService.State.Connected -> WCSessionViewModel.Status.ONLINE
            is WC2PingService.State.Disconnected -> WCSessionViewModel.Status.OFFLINE
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

    private fun setButtons(
        state: WC2SessionService.State,
        connection: WC2PingService.State
    ) {
        buttonStates = WCSessionButtonStates(
            connect = getConnectButtonState(state, connection),
            disconnect = getDisconnectButtonState(state, connection),
            cancel = getCancelButtonState(state),
            reconnect = getReconnectButtonState(connection)
        )
    }

    private fun setError(
        state: WC2SessionService.State
    ) {
        val error: String? = when (state) {
            is WC2SessionService.State.Invalid -> state.error.message ?: state.error::class.java.simpleName
            else -> null
        }

        showError = error
    }

    private fun getHint(connection: WC2PingService.State, state: WC2SessionService.State): Int? =
        when {
            connection is WC2PingService.State.Disconnected -> R.string.WalletConnect_Reconnect_Hint
            connection != WC2PingService.State.Connected -> null
            state == WC2SessionService.State.WaitingForApproveSession -> R.string.WalletConnect_Approve_Hint
            state == WC2SessionService.State.Ready -> R.string.WalletConnect_Ready_Hint
            else -> null
        }
}

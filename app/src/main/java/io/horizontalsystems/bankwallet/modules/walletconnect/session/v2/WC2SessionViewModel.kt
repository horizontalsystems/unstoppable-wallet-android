package io.horizontalsystems.bankwallet.modules.walletconnect.session.v2

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WCSessionModule
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WCSessionViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2SessionService.State.*
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2PingService.State
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager.RequestDataError.*
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class WC2SessionViewModel(private val service: WC2SessionService) : ViewModel() {

    private val TAG = "WC2SessionViewModel"

    val closeLiveEvent = SingleLiveEvent<Unit>()
    val showErrorLiveEvent = SingleLiveEvent<Unit>()

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

    var blockchains by mutableStateOf<List<WC2SessionModule.BlockchainViewItem>>(listOf())
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

        service.allowedBlockchainsObservable
            .subscribeIO {
                sync(allowedBlockchainList = it)
            }
            .let {
                disposables.add(it)
            }

        service.networkConnectionErrorObservable
            .subscribeIO {
                showErrorLiveEvent.postValue(Unit)
            }
            .let {
                disposables.add(it)
            }

        service.start()
    }

    override fun onCleared() {
        service.stop()
        disposables.clear()
    }

    private fun sync(
        sessionState: WC2SessionService.State? = null,
        connectionState: State? = null,
        allowedBlockchainList: List<WCBlockchain>? = null
    ) {
        val state = sessionState ?: service.state
        val connection = connectionState ?: service.connectionState
        val allowedBlockchains = allowedBlockchainList ?: service.allowedBlockchains

        if (state == Killed) {
            closeLiveEvent.postValue(Unit)
            return
        }

        peerMeta = service.appMetaItem
        blockchains = getBlockchainViewItems(allowedBlockchains, peerMeta?.editable ?: false)
        connecting = connection == State.Connecting
        closeEnabled = state == Ready
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

    fun toggle(chainId: Int) {
        service.toggle(chainId)
    }

    private fun getBlockchainViewItems(
        blockchains: List<WCBlockchain>,
        editable: Boolean
    ): List<WC2SessionModule.BlockchainViewItem> {
        return blockchains.map {
            WC2SessionModule.BlockchainViewItem(
                it.chainId,
                it.name,
                getShortened(it.address),
                it.selected,
                showCheckbox = editable
            )
        }
    }

    private fun getShortened(address: String): String = if (address.length > 10) {
        "${address.take(5)}...${address.takeLast(5)}"
    } else {
        address
    }

    private fun getStatus(connectionState: State): WCSessionViewModel.Status? {
        return when (connectionState) {
            State.Connecting -> WCSessionViewModel.Status.CONNECTING
            State.Connected -> WCSessionViewModel.Status.ONLINE
            is State.Disconnected -> WCSessionViewModel.Status.OFFLINE
        }
    }

    private fun setButtons(
        state: WC2SessionService.State,
        connection: State
    ) {
        Log.e(TAG, "setButtons: ${state}, ${connection}")
        buttonStates = WCSessionButtonStates(
            connect = getConnectButtonState(state, connection),
            disconnect = getDisconnectButtonState(state, connection),
            cancel = getCancelButtonState(state),
            reconnect = getReconnectButtonState(state, connection),
            remove = getRemoveButtonState(state, connection),
        )
    }

    private fun getCancelButtonState(state: WC2SessionService.State): WCButtonState {
        return if (state != Ready) {
            WCButtonState.Enabled
        } else {
            WCButtonState.Hidden
        }
    }

    private fun getConnectButtonState(
        state: WC2SessionService.State,
        connectionState: State
    ): WCButtonState {
        return when {
            state == WaitingForApproveSession && connectionState == State.Connected -> WCButtonState.Enabled
            else -> WCButtonState.Hidden
        }
    }

    private fun getDisconnectButtonState(
        state: WC2SessionService.State,
        connectionState: State
    ): WCButtonState {
        return when {
            state == Ready && connectionState == State.Connected -> WCButtonState.Enabled
            else -> WCButtonState.Hidden
        }
    }

    private fun getReconnectButtonState(
        state: WC2SessionService.State,
        connectionState: State
    ): WCButtonState {
        return when {
            state is Invalid -> WCButtonState.Hidden
            connectionState is State.Disconnected -> WCButtonState.Enabled
            connectionState is State.Connecting -> WCButtonState.Disabled
            else -> WCButtonState.Hidden
        }
    }

    private fun getRemoveButtonState(
        state: WC2SessionService.State,
        connectionState: State
    ): WCButtonState {
        return when {
            state is Invalid -> WCButtonState.Hidden
            connectionState is State.Disconnected && state is Ready -> WCButtonState.Enabled
            else -> WCButtonState.Hidden
        }
    }

    private fun setError(
        state: WC2SessionService.State
    ) {
        val error: String? = when (state) {
            is Invalid -> state.error.message ?: state.error::class.java.simpleName
            else -> null
        }

        showError = error
    }

    private fun getHint(connection: State, state: WC2SessionService.State): Int? {
        when {
            connection is State.Disconnected
                    && (state == WaitingForApproveSession || state is Ready) -> {
                return R.string.WalletConnect_Reconnect_Hint
            }
            connection == State.Connecting -> return null
            state is Invalid -> return getErrorMessage(state.error)
            state == WaitingForApproveSession -> R.string.WalletConnect_Approve_Hint
        }
        return null
    }

    private fun getErrorMessage(error: Throwable): Int? {
        return when (error) {
            is UnsupportedChainId -> R.string.WalletConnect_Error_UnsupportedChainId
            is NoSuitableAccount -> R.string.WalletConnect_Error_NoSuitableAccount
            is NoSuitableEvmKit -> R.string.WalletConnect_Error_NoSuitableEvmKit
            is DataParsingError -> R.string.WalletConnect_Error_DataParsingError
            is RequestNotFoundError -> R.string.WalletConnect_Error_RequestNotFoundError
            else -> null
        }
    }
}

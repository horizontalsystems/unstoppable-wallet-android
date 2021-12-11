package io.horizontalsystems.bankwallet.modules.walletconnect.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.WalletConnectInteractor
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectRequest
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectService
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import java.net.UnknownHostException

class WalletConnectMainViewModel(private val service: WalletConnectService) : ViewModel() {

    val connectingLiveData = MutableLiveData<Boolean>()
    val peerMetaLiveData = MutableLiveData<PeerMetaViewItem?>()
    val buttonStatesLiveData = MutableLiveData<ButtonStates>()
    val closeVisibleLiveData = MutableLiveData<Boolean>()
    val signedTransactionsVisibleLiveData = MutableLiveData<Boolean>(false)
    val hintLiveData = MutableLiveData<Int?>()
    val errorLiveData = MutableLiveData<String?>()
    val statusLiveData = MutableLiveData<Status?>()
    val closeLiveEvent = SingleLiveEvent<Unit>()
    val openRequestLiveEvent = SingleLiveEvent<WalletConnectRequest>()

    enum class Status {
        OFFLINE, ONLINE, CONNECTING
    }

    enum class ButtonState(val visible: Boolean, val enabled: Boolean) {
        Enabled(true, true), Disabled(true, false), Hidden(false, true)
    }

    data class ButtonStates(
        val connect: ButtonState,
        val disconnect: ButtonState,
        val cancel: ButtonState,
        val reconnect: ButtonState
    )

    private val disposables = CompositeDisposable()

    init {
        sync(service.state, service.connectionState)

        service.stateObservable
                .subscribe {
                    sync(it, service.connectionState)
                }
                .let {
                    disposables.add(it)
                }

        service.connectionStateObservable
                .subscribe {
                    sync(service.state, it)
                }
                .let {
                    disposables.add(it)
                }


        service.requestObservable
                .subscribe {
                    openRequestLiveEvent.postValue(it)
                }
                .let {
                    disposables.add(it)
                }
    }

    fun cancel() {
        if (service.connectionState == WalletConnectInteractor.State.Connected && service.state == WalletConnectService.State.WaitingForApproveSession) {
            service.rejectSession()
        } else {
            closeLiveEvent.postValue(Unit)
        }
    }

    fun connect() {
        service.approveSession()
    }

    fun disconnect() {
        service.killSession()
    }

    fun reconnect() {
        service.reconnect()
    }

    private fun sync(state: WalletConnectService.State, connectionState: WalletConnectInteractor.State) {
        if (state == WalletConnectService.State.Killed) {
            closeLiveEvent.postValue(Unit)
            return
        }

        val peerMetaViewItem = service.remotePeerMeta?.let { peerMeta ->
            PeerMetaViewItem(peerMeta.name, peerMeta.url, peerMeta.description, peerMeta.icons.lastOrNull())
        }
        peerMetaLiveData.postValue(peerMetaViewItem)

        connectingLiveData.postValue(connectionState == WalletConnectInteractor.State.Connecting)
        closeVisibleLiveData.postValue(state == WalletConnectService.State.Ready)

        val cancelBtnState = getCancelButtonState(state)
        val connectBtnState = getConnectButtonState(state, connectionState)
        val disconnectBtnState = getDisconnectButtonState(state, connectionState)
        val reconnectBtnState = getReconnectButtonState(connectionState)

        buttonStatesLiveData.postValue(ButtonStates(connectBtnState, disconnectBtnState, cancelBtnState, reconnectBtnState))

        statusLiveData.postValue(getStatus(connectionState))


        val hint = when {
            connectionState is WalletConnectInteractor.State.Disconnected -> R.string.WalletConnect_Reconnect_Hint
            connectionState != WalletConnectInteractor.State.Connected -> null
            state == WalletConnectService.State.WaitingForApproveSession -> R.string.WalletConnect_Approve_Hint
            state == WalletConnectService.State.Ready -> R.string.WalletConnect_Ready_Hint
            else -> null
        }

        hintLiveData.postValue(hint)

        val error = if (connectionState is WalletConnectInteractor.State.Disconnected) {
            if (connectionState.error is WalletConnectInteractor.SessionError.SocketDisconnected && connectionState.error.cause is UnknownHostException)
                Translator.getString(R.string.Hud_Text_NoInternet)
            else
                connectionState.error.message ?: connectionState.error::class.java.simpleName
        } else {
            null
        }
        errorLiveData.postValue(error)
    }

    private fun getStatus(connectionState: WalletConnectInteractor.State): Status? {
        return if (service.remotePeerMeta == null) {
            null
        } else {
            when (connectionState) {
                WalletConnectInteractor.State.Connecting -> Status.CONNECTING
                WalletConnectInteractor.State.Connected -> Status.ONLINE
                is WalletConnectInteractor.State.Disconnected -> Status.OFFLINE
                WalletConnectInteractor.State.Idle -> null
            }
        }
    }

    private fun getCancelButtonState(state: WalletConnectService.State): ButtonState {
        return if (state != WalletConnectService.State.Ready){
            ButtonState.Enabled
        } else {
            ButtonState.Hidden
        }
    }

    private fun getConnectButtonState(state: WalletConnectService.State, connectionState: WalletConnectInteractor.State): ButtonState {
        return when {
            state == WalletConnectService.State.WaitingForApproveSession &&
                    connectionState == WalletConnectInteractor.State.Connected -> ButtonState.Enabled
            else -> ButtonState.Hidden
        }
    }

    private fun getDisconnectButtonState(state: WalletConnectService.State, connectionState: WalletConnectInteractor.State): ButtonState {
        return when {
            state == WalletConnectService.State.Ready &&
                    connectionState == WalletConnectInteractor.State.Connected -> ButtonState.Enabled
            else -> ButtonState.Hidden
        }
    }

    private fun getReconnectButtonState(connectionState: WalletConnectInteractor.State): ButtonState {
        return when (connectionState) {
            is WalletConnectInteractor.State.Disconnected -> ButtonState.Enabled
            is WalletConnectInteractor.State.Connecting -> ButtonState.Disabled
            else -> ButtonState.Hidden
        }
    }

}

data class PeerMetaViewItem(val name: String, val url: String, val description: String?, val icon: String?)

package io.horizontalsystems.bankwallet.modules.walletconnect.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.WalletConnectInteractor
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectRequest
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectService
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

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

    data class ButtonStates(val connect: ButtonState, val disconnect: ButtonState, val cancel: ButtonState)

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

    private fun sync(state: WalletConnectService.State, connectionState: WalletConnectInteractor.State) {
        if (state == WalletConnectService.State.Killed) {
            closeLiveEvent.postValue(Unit)
            return
        }

        val peerMetaViewItem = service.remotePeerMeta?.let { peerMeta ->
            PeerMetaViewItem(peerMeta.name, peerMeta.url, peerMeta.description, peerMeta.icons.lastOrNull())
        }
        peerMetaLiveData.postValue(peerMetaViewItem)

        connectingLiveData.postValue(state == WalletConnectService.State.Idle)
        closeVisibleLiveData.postValue(state == WalletConnectService.State.Ready)

        val cancelBtnState = getCancelButtonState(state)
        val connectBtnState = getConnectButtonState(state, connectionState)
        val disconnectBtnState = getDisconnectButtonState(state, connectionState)

        buttonStatesLiveData.postValue(ButtonStates(connectBtnState, disconnectBtnState, cancelBtnState))

        statusLiveData.postValue(getStatus(connectionState))


        val hint = when (state) {
            WalletConnectService.State.WaitingForApproveSession -> R.string.WalletConnect_Approve_Hint
            WalletConnectService.State.Ready -> R.string.WalletConnect_Ready_Hint
            else -> null
        }

        hintLiveData.postValue(hint)

        val error = if (state is WalletConnectService.State.Invalid) {
            state.error.message ?: state.error.javaClass.simpleName
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
            state != WalletConnectService.State.WaitingForApproveSession -> ButtonState.Hidden
            connectionState == WalletConnectInteractor.State.Connected -> ButtonState.Enabled
            else -> ButtonState.Disabled
        }
    }

    private fun getDisconnectButtonState(state: WalletConnectService.State, connectionState: WalletConnectInteractor.State): ButtonState {
        return when {
            state != WalletConnectService.State.Ready -> ButtonState.Hidden
            connectionState == WalletConnectInteractor.State.Connected -> ButtonState.Enabled
            else -> ButtonState.Disabled
        }
    }

}

data class PeerMetaViewItem(val name: String, val url: String, val description: String?, val icon: String?)

package io.horizontalsystems.bankwallet.modules.walletconnect.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectRequest
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectService
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class WalletConnectMainViewModel(private val service: WalletConnectService) : ViewModel() {

    val connectingLiveData = MutableLiveData<Boolean>()
    val peerMetaLiveData = MutableLiveData<PeerMetaViewItem?>()
    val cancelVisibleLiveData = MutableLiveData<Boolean>()
    val approveAndRejectVisibleLiveData = MutableLiveData<Boolean>()
    val closeVisibleLiveData = MutableLiveData<Boolean>()
    val disconnectVisibleLiveData = MutableLiveData<Boolean>()
    val signedTransactionsVisibleLiveData = MutableLiveData<Boolean>()
    val hintLiveData = MutableLiveData<Int?>()
    val statusLiveData = MutableLiveData<Status?>()
    val closeLiveEvent = SingleLiveEvent<Unit>()
    val openRequestLiveEvent = SingleLiveEvent<WalletConnectRequest>()

    enum class Status {
        OFFLINE, ONLINE, CONNECTING
    }

    private val disposables = CompositeDisposable()

    init {
        syncState(service.state)

        service.stateSubject
                .subscribe {
                    syncState(it)
                }
                .let {
                    disposables.add(it)
                }

        service.requestSubject
                .subscribe {
                    openRequestLiveEvent.postValue(it)
                }
                .let {
                    disposables.add(it)
                }
    }

    fun approve() {
        service.approveSession()
    }

    fun reject() {
        service.rejectSession()
    }

    fun disconnect() {
        service.killSession()
    }

    private fun syncState(state: WalletConnectService.State) {
        if (state == WalletConnectService.State.Completed) {
            closeLiveEvent.postValue(Unit)
            return
        }

        val peerMetaViewItem = service.remotePeerMeta?.let { peerMeta ->
            PeerMetaViewItem(peerMeta.name, peerMeta.url, peerMeta.description, peerMeta.icons.lastOrNull())
        }
        peerMetaLiveData.postValue(peerMetaViewItem)

        connectingLiveData.postValue(state == WalletConnectService.State.Connecting && service.remotePeerMeta == null)
        cancelVisibleLiveData.postValue(state == WalletConnectService.State.Connecting)
        disconnectVisibleLiveData.postValue(state == WalletConnectService.State.Ready)
        closeVisibleLiveData.postValue(state == WalletConnectService.State.Ready)
        approveAndRejectVisibleLiveData.postValue(state == WalletConnectService.State.WaitingForApproveSession)
        signedTransactionsVisibleLiveData.postValue(state == WalletConnectService.State.Ready)

        statusLiveData.postValue(when (state) {
            WalletConnectService.State.Connecting -> Status.CONNECTING
            WalletConnectService.State.Ready -> Status.ONLINE
            else -> null
        })


        val hint = when (state) {
            WalletConnectService.State.WaitingForApproveSession -> R.string.WalletConnect_Approve_Hint
            WalletConnectService.State.Ready -> R.string.WalletConnect_Ready_Hint
            else -> null
        }

        hintLiveData.postValue(hint)
    }
}

data class PeerMetaViewItem(val name: String, val url: String, val description: String?, val icon: String?)

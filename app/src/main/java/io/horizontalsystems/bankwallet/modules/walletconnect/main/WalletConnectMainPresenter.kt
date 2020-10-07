package io.horizontalsystems.bankwallet.modules.walletconnect.main

import android.util.Log
import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectService
import io.reactivex.disposables.CompositeDisposable

class WalletConnectMainPresenter(private val service: WalletConnectService) {

    val connectingLiveData = MutableLiveData<Boolean>()
    val peerMetaLiveData = MutableLiveData<PeerMetaViewItem?>()
    val cancelVisibleLiveData = MutableLiveData<Boolean>()
    val approveAndRejectVisibleLiveData = MutableLiveData<Boolean>()
    val disconnectVisibleLiveData = MutableLiveData<Boolean>()
    val signedTransactionsVisibleLiveData = MutableLiveData<Boolean>()
    val hintLiveData = MutableLiveData<Int?>()
    val stateLiveData = MutableLiveData<State?>()

    enum class State {
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
    }

    fun approve() {
        service.approveSession()
    }

    fun reject() {
        service.rejectSession()
    }

    private fun syncState(state: WalletConnectService.State) {
        Log.e("AAA", "state $state")

        val peerMetaViewItem = service.peerMeta?.let { peerMeta ->
            PeerMetaViewItem(peerMeta.name, peerMeta.url, peerMeta.description, peerMeta.icons.lastOrNull())
        }
        peerMetaLiveData.postValue(peerMetaViewItem)

        connectingLiveData.postValue(state == WalletConnectService.State.Connecting && service.peerMeta == null)
        cancelVisibleLiveData.postValue(state == WalletConnectService.State.Connecting)
        disconnectVisibleLiveData.postValue(state == WalletConnectService.State.Ready)
        approveAndRejectVisibleLiveData.postValue(state == WalletConnectService.State.WaitingForApproveSession)
        signedTransactionsVisibleLiveData.postValue(state == WalletConnectService.State.Ready)

        stateLiveData.postValue(when (state) {
            WalletConnectService.State.Connecting -> State.CONNECTING
            WalletConnectService.State.Ready -> State.ONLINE
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

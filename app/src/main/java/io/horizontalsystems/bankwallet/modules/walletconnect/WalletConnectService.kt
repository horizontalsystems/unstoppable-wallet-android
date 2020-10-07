package io.horizontalsystems.bankwallet.modules.walletconnect

import com.trustwallet.walletconnect.WCSessionStoreType
import com.trustwallet.walletconnect.models.WCPeerMeta
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IEthereumKitManager
import io.horizontalsystems.bankwallet.core.managers.WalletConnectInteractor
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.subjects.PublishSubject

class WalletConnectService(ethKitManager: IEthereumKitManager) : WalletConnectInteractor.Delegate, Clearable {

    sealed class State {
        object Idle : State()
        object Connecting : State()
        object WaitingForApproveSession : State()
        object Ready : State()
        object Rejected : State()
    }

    private val ethereumKit: EthereumKit? = ethKitManager.ethereumKit
    private var interactor: WalletConnectInteractor? = null
    var peerMeta: WCPeerMeta? = null

    var state: State = State.Connecting
        private set(value) {
            field = value

            stateSubject.onNext(value)
        }

    val stateSubject = PublishSubject.create<State>()

    val isEthereumKitReady: Boolean
        get() = ethereumKit != null

    init {
        val wcSessionStoreType = WCSessionStoreType(App.preferences)

        val sessionStoreItem = wcSessionStoreType.session
        if (sessionStoreItem != null) {
            peerMeta = sessionStoreItem.remotePeerMeta

            interactor = WalletConnectInteractor(sessionStoreItem.session, sessionStoreItem.peerId, sessionStoreItem.remotePeerId)
            interactor?.delegate = this
            interactor?.connect()

            state = State.Connecting
        } else {
            state = State.Idle
        }
    }


    fun connect(uri: String) {
        interactor = WalletConnectInteractor(uri)
        interactor?.delegate = this
        interactor?.connect()

        state = State.Connecting
    }

    override fun clear() = Unit

    fun approveSession() {
        ethereumKit?.let { ethereumKit ->
            interactor?.let {
                it.approveSession(ethereumKit.receiveAddress.eip55, ethereumKit.networkType.getNetwork().id)

                state = State.Ready
            }
        }
    }

    fun rejectSession() {
        interactor?.let {
            it.rejectSession()

            state = State.Rejected
        }
    }

    override fun didRequestSession(peerMeta: WCPeerMeta) {
        this.peerMeta = peerMeta

        state = State.WaitingForApproveSession
    }
}

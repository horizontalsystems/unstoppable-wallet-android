package io.horizontalsystems.bankwallet.modules.walletconnect

import com.trustwallet.walletconnect.WCSessionStoreItem
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
        object Completed : State()
    }

    private val ethereumKit: EthereumKit? = ethKitManager.ethereumKit
    private var interactor: WalletConnectInteractor? = null
    private var remotePeerData: PeerData? = null
    val remotePeerMeta: WCPeerMeta?
        get() = remotePeerData?.peerMeta

    var state: State = State.Connecting
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }

    val stateSubject = PublishSubject.create<State>()

    val isEthereumKitReady: Boolean
        get() = ethereumKit != null

    private val wcSessionStoreType = WCSessionStoreType(App.preferences)

    init {
        val sessionStoreItem = wcSessionStoreType.session
        if (sessionStoreItem != null) {
            remotePeerData = PeerData(sessionStoreItem.remotePeerId, sessionStoreItem.remotePeerMeta)

            interactor = WalletConnectInteractor(sessionStoreItem.session, sessionStoreItem.peerId)
            interactor?.delegate = this
            interactor?.connect(sessionStoreItem.remotePeerId)

            state = State.Connecting
        } else {
            state = State.Idle
        }
    }


    fun connect(uri: String) {
        interactor = WalletConnectInteractor(uri)
        interactor?.delegate = this
        interactor?.connect(null)

        state = State.Connecting
    }

    override fun clear() = Unit

    fun approveSession() {
        ethereumKit?.let { ethereumKit ->
            interactor?.let { interactor ->
                interactor.approveSession(ethereumKit.receiveAddress.eip55, ethereumKit.networkType.getNetwork().id)

                remotePeerData?.let { peerData ->
                    wcSessionStoreType.session = WCSessionStoreItem(interactor.session, interactor.peerId, peerData.peerId, peerData.peerMeta)
                }

                state = State.Ready
            }
        }
    }

    fun rejectSession() {
        interactor?.let {
            it.rejectSession()

            state = State.Completed
        }
    }

    fun killSession() {
        interactor?.let {
            it.killSession()
            wcSessionStoreType.session = null

            state = State.Completed
        }
    }

    override fun didConnect() {
        if (remotePeerData != null) {
            state = State.Ready
        }
    }

    override fun didRequestSession(remotePeerId: String, remotePeerMeta: WCPeerMeta) {
        this.remotePeerData = PeerData(remotePeerId, remotePeerMeta)

        state = State.WaitingForApproveSession
    }

    data class PeerData(val peerId: String, val peerMeta: WCPeerMeta)

}

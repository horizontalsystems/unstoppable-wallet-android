package io.horizontalsystems.bankwallet.modules.walletconnect

import com.trustwallet.walletconnect.WCSessionStoreItem
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IEthereumKitManager
import io.horizontalsystems.bankwallet.core.managers.WalletConnectInteractor
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.subjects.PublishSubject

class WalletConnectService(
        ethKitManager: IEthereumKitManager,
        private val sessionStore: WalletConnectSessionStore
) : WalletConnectInteractor.Delegate, Clearable {

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
    val requestSubject = PublishSubject.create<Long>()

    val isEthereumKitReady: Boolean
        get() = ethereumKit != null

    private val pendingRequests = mutableMapOf<Long, Request>()

    init {
        val sessionStoreItem = sessionStore.storedItem

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

    override fun clear() {
        interactor?.disconnect()
    }

    fun approveSession() {
        ethereumKit?.let { ethereumKit ->
            interactor?.let { interactor ->
                val chainId = ethereumKit.networkType.getNetwork().id
                interactor.approveSession(ethereumKit.receiveAddress.eip55, chainId)

                remotePeerData?.let { peerData ->
                    sessionStore.storedItem = WCSessionStoreItem(interactor.session, chainId, interactor.peerId, peerData.peerId, peerData.peerMeta)
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
        interactor?.killSession()
    }

    fun getRequest(requestId: Long) : Request? {
        return pendingRequests[requestId]
    }

    fun approveRequest(requestId: Long) {
        TODO("Not yet implemented")
    }

    fun rejectRequest(requestId: Long) {
        pendingRequests.remove(requestId)

        interactor?.rejectRequest(requestId, "Rejected by user")
    }

    override fun didConnect() {
        if (remotePeerData != null) {
            state = State.Ready
        }
    }

    override fun didKillSession() {
        sessionStore.storedItem = null

        state = State.Completed
    }

    override fun didRequestSession(remotePeerId: String, remotePeerMeta: WCPeerMeta) {
        this.remotePeerData = PeerData(remotePeerId, remotePeerMeta)

        state = State.WaitingForApproveSession
    }

    override fun didRequestSendEthTransaction(id: Long, transaction: WCEthereumTransaction) {
        didRequest(Request(id, Request.Type.SendEthereumTransaction(transaction)))
    }

    override fun didRequestSignEthTransaction(id: Long, transaction: WCEthereumTransaction) {
        didRequest(Request(id, Request.Type.SignEthereumTransaction(transaction)))
    }

    private fun didRequest(request: Request) {
        pendingRequests[request.id] = request
        requestSubject.onNext(request.id)
    }

    data class PeerData(val peerId: String, val peerMeta: WCPeerMeta)

    data class Request(val id: Long, val type: Type) {
        sealed class Type {
            data class SendEthereumTransaction(val transaction: WCEthereumTransaction) : Type()
            data class SignEthereumTransaction(val transaction: WCEthereumTransaction) : Type()
        }
    }

}

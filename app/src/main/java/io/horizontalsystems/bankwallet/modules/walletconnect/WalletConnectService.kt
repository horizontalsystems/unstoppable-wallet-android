package io.horizontalsystems.bankwallet.modules.walletconnect

import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.EthereumKitManager
import io.horizontalsystems.bankwallet.core.managers.WalletConnectInteractor
import io.horizontalsystems.bankwallet.entities.WalletConnectSession
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class WalletConnectService(
        ethKitManager: EthereumKitManager,
        private val sessionManager: WalletConnectSessionManager,
        private val connectivityManager: ConnectivityManager
) : WalletConnectInteractor.Delegate, Clearable {

    sealed class State {
        object Idle : State()
        object WaitingForApproveSession : State()
        object Ready : State()
        object Killed : State()
    }

    data class PeerData(val peerId: String, val peerMeta: WCPeerMeta)

    private val ethereumKit: EthereumKit? = ethKitManager.evmKit
    private var interactor: WalletConnectInteractor? = null
    private var remotePeerData: PeerData? = null
    val remotePeerMeta: WCPeerMeta?
        get() = remotePeerData?.peerMeta

    var state: State = State.Idle
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }

    val connectionState: WalletConnectInteractor.State
        get() = interactor?.state ?: WalletConnectInteractor.State.Disconnected

    val stateSubject = PublishSubject.create<State>()
    val connectionStateSubject = PublishSubject.create<WalletConnectInteractor.State>()
    val requestSubject = PublishSubject.create<WalletConnectRequest>()

    val isEthereumKitReady: Boolean
        get() = ethereumKit != null

    private val pendingRequests = linkedMapOf<Long, WalletConnectRequest>()
    private val disposable = CompositeDisposable()
    private var requestIsProcessing = false

    init {
        val sessionStoreItem = sessionManager.storedSession

        if (sessionStoreItem != null) {
            remotePeerData = PeerData(sessionStoreItem.remotePeerId, sessionStoreItem.remotePeerMeta)

            interactor = WalletConnectInteractor(sessionStoreItem.session, sessionStoreItem.peerId, sessionStoreItem.remotePeerId)
            interactor?.delegate = this
            interactor?.connect()

            state = State.Ready
        } else {
            state = State.Idle
        }

        connectivityManager.networkAvailabilitySignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    if (connectivityManager.isConnected) {
                        interactor?.connect()
                    }
                }
                .let {
                    disposable.add(it)
                }
    }


    fun connect(uri: String) {
        interactor = WalletConnectInteractor(uri)
        interactor?.delegate = this
        interactor?.connect()
    }

    override fun clear() {
        disposable.clear()
        interactor?.disconnect()
    }

    fun approveSession() {
        ethereumKit?.let { ethereumKit ->
            interactor?.let { interactor ->
                val chainId = ethereumKit.networkType.getNetwork().id
                interactor.approveSession(ethereumKit.receiveAddress.eip55, chainId)

                remotePeerData?.let { peerData ->
                    sessionManager.store(WalletConnectSession(chainId, "", interactor.session, interactor.peerId, peerData.peerId, peerData.peerMeta))
                }

                state = State.Ready
            }
        }
    }

    fun rejectSession() {
        interactor?.let {
            it.rejectSession()

            state = State.Killed
        }
    }

    fun killSession() {
        interactor?.killSession()
    }

    fun approveRequest(requestId: Long, result: Any) {
        val request = pendingRequests.remove(requestId)

        request?.let {
            interactor?.approveRequest(requestId, it.convertResult(result))
        }

        requestIsProcessing = false
        processNextRequest()
    }

    fun rejectRequest(requestId: Long) {
        pendingRequests.remove(requestId)

        interactor?.rejectRequest(requestId, "Rejected by user")

        requestIsProcessing = false
        processNextRequest()
    }

    override fun didUpdateState(state: WalletConnectInteractor.State) {
        connectionStateSubject.onNext(state)
    }

    override fun didKillSession() {
        sessionManager.clear()

        state = State.Killed
    }

    override fun didRequestSession(remotePeerId: String, remotePeerMeta: WCPeerMeta) {
        this.remotePeerData = PeerData(remotePeerId, remotePeerMeta)

        state = State.WaitingForApproveSession
    }

    override fun didRequestSendEthTransaction(id: Long, transaction: WCEthereumTransaction) {
        handleRequest(id) {
            WalletConnectSendEthereumTransactionRequest(id, transaction)
        }
    }

    private fun handleRequest(id: Long, requestResolver: () -> WalletConnectRequest) {
        try {
            val request = requestResolver()
            pendingRequests[request.id] = request

            processNextRequest()
        } catch (t: Throwable) {
            interactor?.rejectRequest(id, t.message ?: "")
        }
    }

    @Synchronized
    private fun processNextRequest() {
        if (requestIsProcessing) return

        pendingRequests.values.firstOrNull()?.let {
            requestSubject.onNext(it)
            requestIsProcessing = true
        }
    }
}

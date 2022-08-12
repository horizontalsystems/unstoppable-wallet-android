package io.horizontalsystems.bankwallet.modules.walletconnect.version1

import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.managers.WalletConnectInteractor
import io.horizontalsystems.bankwallet.core.order
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.walletconnect.entity.WalletConnectSession
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WCSessionModule.WCRequestWrapper
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.marketkit.models.Blockchain
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class WC1Service(
    private val remotePeerId: String?,
    private val connectionLink: String?,
    private val manager: WC1Manager,
    private val sessionManager: WC1SessionManager,
    private val requestManager: WC1RequestManager,
    private val connectivityManager: ConnectivityManager,
    private val evmBlockchainManager: EvmBlockchainManager
) : WalletConnectInteractor.Delegate, Clearable {

    sealed class State {
        object Idle : State()
        class Invalid(val error: Throwable) : State()
        object WaitingForApproveSession : State()
        object Ready : State()
        object Killed : State()
    }

    data class SessionData(
        val peerId: String,
        val peerMeta: WCPeerMeta,
        val account: Account
    )

    open class SessionError(message: String) : Throwable(message) {
        object UnsupportedChainId : SessionError("Unsupported chain id")
        object NoSuitableAccount : SessionError("No suitable account")
    }

    private var interactor: WalletConnectInteractor? = null
    private val disposable = CompositeDisposable()
    private var requestIsProcessing = false
    private var sessionData: SessionData? = null

    private val peerId: String?
        get() = sessionData?.peerId

    val remotePeerMeta: WCPeerMeta?
        get() = sessionData?.peerMeta

    val evmKitWrapper: EvmKitWrapper?
        get() = sessionData?.account?.let { account ->
                manager.getEvmKitWrapper(evmBlockchainManager.getChain(selectedBlockchain.type).id, account)
            }

    val evmAddress: Address?
        get() = evmKitWrapper?.evmKit?.receiveAddress

    var selectedBlockchain: Blockchain = evmBlockchainManager.getBlockchain(Chain.Ethereum.id)!!
    val availableBlockchains: List<Blockchain>
        get() = evmBlockchainManager.allBlockchains.sortedBy { it.type.order }

    private val stateSubject = PublishSubject.create<State>()
    val stateObservable: Flowable<State>
        get() = stateSubject.toFlowable(BackpressureStrategy.BUFFER)

    var state: State = State.Idle
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }

    private val connectionStateSubject = PublishSubject.create<WalletConnectInteractor.State>()
    val connectionStateObservable: Flowable<WalletConnectInteractor.State>
        get() = connectionStateSubject.toFlowable(BackpressureStrategy.BUFFER)

    val connectionState: WalletConnectInteractor.State
        get() = interactor?.state ?: WalletConnectInteractor.State.Idle

    private val requestSubject = PublishSubject.create<WCRequestWrapper>()
    val requestObservable: Flowable<WCRequestWrapper>
        get() = requestSubject.toFlowable(BackpressureStrategy.BUFFER)

    fun start() {
        remotePeerId?.let {
            val session = sessionManager.sessions.firstOrNull { session -> session.remotePeerId == remotePeerId }
            if (session != null) {
                restoreSession(session)
            }
        }

        connectionLink?.let{
            try {
                connect(it)
            } catch (t: Throwable) {
                state = State.Invalid(t)
            }
        }

        connectivityManager.networkAvailabilitySignal
                .subscribeIO {
                    if (connectivityManager.isConnected && interactor?.state is WalletConnectInteractor.State.Disconnected) {
                        interactor?.connect()
                    }
                }
                .let {
                    disposable.add(it)
                }
    }

    fun stop() {
        disposable.clear()
    }

    private fun restoreSession(session: WalletConnectSession) {
        try {
            initSession(session.remotePeerId, session.remotePeerMeta, session.chainId)

            interactor = WalletConnectInteractor(session.session, session.peerId, session.remotePeerId)
            interactor?.delegate = this
            interactor?.connect()

            state = State.Ready

            processNextRequest()
        } catch (error: Throwable) {
            state = State.Invalid(error)
        }
    }

    private fun initSession(peerId: String, peerMeta: WCPeerMeta, chainId: Int) {
        selectedBlockchain = evmBlockchainManager.getBlockchain(chainId) ?: throw SessionError.UnsupportedChainId

        val account = manager.activeAccount ?: throw SessionError.NoSuitableAccount
        sessionData = SessionData(peerId, peerMeta, account)
    }

    private fun getSessionFromUri(uri: String): WalletConnectSession? {
        return sessionManager.sessions.firstOrNull { session -> "wc:${session.session.topic}@${session.session.version}" == uri }
    }

    fun connect(uri: String) {
        val session = getSessionFromUri(uri)
        if (session != null) {
            if (sessionData?.peerId != session.remotePeerId) { // session is not current active session
                restoreSession(session)
            }
        } else {
            interactor = WalletConnectInteractor(uri)
            interactor?.delegate = this
            interactor?.connect()
        }
    }

    override fun clear() {
        disposable.clear()
        interactor?.disconnect()
    }

    fun approveSession() {
        sessionData?.let { sessionData ->
            interactor?.let { interactor ->
                evmKitWrapper?.let { evmKitWrapper ->
                    val chainId = evmKitWrapper.evmKit.chain.id
                    interactor.approveSession(evmKitWrapper.evmKit.receiveAddress.eip55, chainId)

                    val session = WalletConnectSession(
                        chainId = chainId,
                        accountId = sessionData.account.id,
                        session = interactor.session,
                        peerId = interactor.peerId,
                        remotePeerId = sessionData.peerId,
                        remotePeerMeta = sessionData.peerMeta
                    )

                    sessionManager.save(session)

                    state = State.Ready
                }
            }
        }
    }

    fun rejectSession() {
        interactor?.let {
            it.rejectSession("Session Rejected by User")

            state = State.Killed
        }
    }

    fun killSession() {
        interactor?.killSession()
    }

    fun approveRequest(requestId: Long, result: Any) {
        val request = peerId?.let { requestManager.remove(it, requestId) }

        request?.let {
            interactor?.approveRequest(requestId, it.convertResult(result))
        }

        requestIsProcessing = false
        processNextRequest()
    }

    fun rejectRequest(requestId: Long) {
        peerId?.let { requestManager.remove(it, requestId) }

        interactor?.rejectRequest(requestId, "Rejected by User")

        requestIsProcessing = false
        processNextRequest()
    }

    fun reconnect() {
        interactor?.connect()
    }

    override fun didUpdateState(state: WalletConnectInteractor.State) {
        connectionStateSubject.onNext(state)
    }

    override fun didKillSession() {
        sessionData?.let {
            sessionManager.deleteSession(it.peerId)
        }

        state = State.Killed
    }

    override fun didRequestSession(remotePeerId: String, remotePeerMeta: WCPeerMeta, chainId: Int?) {
        state = try {
            // if (chainId == null) throw SessionError.UnsupportedChainId

            initSession(remotePeerId, remotePeerMeta, chainId ?: 1) //fall back to Ethereum MainNet

            State.WaitingForApproveSession
        } catch (error: Throwable) {
            interactor?.rejectSession("Session rejected: ${error.message}")

            State.Invalid(error)
        }
    }

    override fun didRequestSendEthTransaction(id: Long, transaction: WCEthereumTransaction) {
        handleRequest(id) {
            WC1SendEthereumTransactionRequest(id, transaction)
        }
    }

    override fun didRequestSignMessage(id: Long, message: WCEthereumSignMessage) {
        handleRequest(id) {
            WC1SignMessageRequest(id, message)
        }
    }

    private fun handleRequest(id: Long, requestResolver: () -> WC1Request) {
        try {
            peerId?.let { peerId ->
                val request = requestResolver()
                requestManager.save(peerId, request)
                processNextRequest()
            }
        } catch (t: Throwable) {
            interactor?.rejectRequest(id, t.message ?: "")
        }
    }

    @Synchronized
    private fun processNextRequest() {
        if (requestIsProcessing) return

        peerId?.let { peerId ->
            requestManager.getNextRequest(peerId)?.let { request ->
                requestSubject.onNext(WCRequestWrapper(request, remotePeerMeta?.name))
                requestIsProcessing = true
            }
        }
    }
}

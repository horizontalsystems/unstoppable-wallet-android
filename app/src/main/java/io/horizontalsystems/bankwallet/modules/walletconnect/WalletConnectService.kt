package io.horizontalsystems.bankwallet.modules.walletconnect

import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.WalletConnectInteractor
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.WalletConnectSession
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class WalletConnectService(
        remotePeerId: String?,
        private val manager: WalletConnectManager,
        private val sessionManager: WalletConnectSessionManager,
        private val connectivityManager: ConnectivityManager
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
            val account: Account,
            val evmKit: EthereumKit
    )

    open class SessionError : Throwable() {
        object UnsupportedChainId : SessionError()
        object NoSuitableAccount : SessionError()
    }

    private var interactor: WalletConnectInteractor? = null
    private val pendingRequests = linkedMapOf<Long, WalletConnectRequest>()
    private val disposable = CompositeDisposable()
    private var requestIsProcessing = false

    private var sessionData: SessionData? = null
    val remotePeerMeta: WCPeerMeta?
        get() = sessionData?.peerMeta

    val evmKit: EthereumKit?
        get() = sessionData?.evmKit

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
        get() = interactor?.state ?: WalletConnectInteractor.State.Disconnected

    private val requestSubject = PublishSubject.create<WalletConnectRequest>()
    val requestObservable: Flowable<WalletConnectRequest>
        get() = requestSubject.toFlowable(BackpressureStrategy.BUFFER)

    init {
        remotePeerId?.let {
            val session = sessionManager.sessions.firstOrNull { session -> session.remotePeerId == remotePeerId }
            if (session != null) {
                restoreSession(session)
            }
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

    private fun restoreSession(session: WalletConnectSession) {
        try {
            initSession(session.remotePeerId, session.remotePeerMeta, session.chainId)

            interactor = WalletConnectInteractor(session.session, session.peerId, session.remotePeerId)
            interactor?.delegate = this
            interactor?.connect()

            state = State.Ready
        } catch (error: Throwable) {
            state = State.Invalid(error)
        }
    }

    private fun initSession(peerId: String, peerMeta: WCPeerMeta, chainId: Int) {
        val account = manager.currentAccount(chainId) ?: throw SessionError.NoSuitableAccount
        val evmKit = manager.evmKit(chainId, account) ?: throw SessionError.UnsupportedChainId

        sessionData = SessionData(peerId, peerMeta, account, evmKit)
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
        sessionData?.let { sessionData ->
            interactor?.let { interactor ->
                val evmKit = sessionData.evmKit
                val chainId = evmKit.networkType.getNetwork().id
                interactor.approveSession(evmKit.receiveAddress.eip55, chainId)

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
        val request = pendingRequests.remove(requestId)

        request?.let {
            interactor?.approveRequest(requestId, it.convertResult(result))
        }

        requestIsProcessing = false
        processNextRequest()
    }

    fun rejectRequest(requestId: Long) {
        pendingRequests.remove(requestId)

        interactor?.rejectRequest(requestId, "Rejected by User")

        requestIsProcessing = false
        processNextRequest()
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

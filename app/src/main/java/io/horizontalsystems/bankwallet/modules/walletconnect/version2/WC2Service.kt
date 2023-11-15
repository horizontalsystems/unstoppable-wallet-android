package io.horizontalsystems.bankwallet.modules.walletconnect.version2

import android.util.Log
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WCBlockchain
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

class WC2Service : SignClient.WalletDelegate {

    private val TAG = "WC2Service"

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val eventSubject = PublishSubject.create<Event>()
    val eventObservable: Flowable<Event>
        get() = eventSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val sessionsUpdatedSubject = PublishSubject.create<Unit>()
    val sessionsUpdatedObservable: Flowable<Unit>
        get() = sessionsUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val sessionsRequestReceivedSubject =
        PublishSubject.create<Sign.Model.SessionRequest>()
    val sessionsRequestReceivedObservable: Flowable<Sign.Model.SessionRequest>
        get() = sessionsRequestReceivedSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val pendingRequestUpdatedSubject = PublishSubject.create<Unit>()
    val pendingRequestUpdatedObservable: Flowable<Unit>
        get() = pendingRequestUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    val activeSessions: List<Sign.Model.Session>
        get() = SignClient.getListOfActiveSessions()

    private val _connectionAvailableStateFlow: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    val connectionAvailableStateFlow: StateFlow<Boolean?>
        get() = _connectionAvailableStateFlow.asStateFlow()

    init {
        SignClient.setWalletDelegate(this)
    }

    fun pendingRequests(topic: String): List<Sign.Model.PendingRequest> {
        return SignClient.getPendingRequests(topic)
    }

    var event: Event = Event.Default
        private set(value) {
            field = value
            eventSubject.onNext(value)
        }

    sealed class Event {
        object Default : Event()
        class Error(val error: Throwable) : Event()
        object WaitingForApproveSession : Event()
        class SessionSettled(val session: Sign.Model.Session) : Event()
        class SessionDeleted(val deletedSession: Sign.Model.DeletedSession) : Event()
    }

    fun start() {
        sessionsUpdatedSubject.onNext(Unit)
    }

    fun pair(uri: String) {
        val pair = Core.Params.Pair(uri.trim())
        CoreClient.Pairing.pair(pair) { error ->
            Log.e(TAG, "pair onError: ", error.throwable)
            event = Event.Error(error.throwable)
        }
    }

    fun getPairings(): List<Core.Model.Pairing> {
        return CoreClient.Pairing.getPairings()
    }

    fun deletePairing(topic: String) {
        CoreClient.Pairing.disconnect(Core.Params.Disconnect(topic)) { error ->
            Log.e(TAG, "disconnect pair error: ", error.throwable)
            event = Event.Error(error.throwable)
        }
    }

    fun deleteAllPairings() {
        getPairings().forEach {
            deletePairing(it.topic)
        }
    }

    fun approve(proposal: Sign.Model.SessionProposal, blockchains: List<WCBlockchain>) {
        val supportedMethods = listOf(
            "personal_sign",
            "eth_signTypedData",
            "eth_sendTransaction",
            "eth_sign",
        )

        val namespaces = proposal.requiredNamespaces + proposal.optionalNamespaces
        val methods = namespaces.values
            .flatMap { it.methods }
            .distinct()
            .filter { supportedMethods.contains(it) }
        val events = namespaces.values.flatMap { it.events }.distinct()

        val sessionNamespaces = blockchains
            .groupBy { it.chainNamespace }
            .mapValues { (_, blockchains) ->
                Sign.Model.Namespace.Session(
                    accounts = blockchains.map(WCBlockchain::getAccount),
                    methods = methods,
                    events = events
                )
            }
            .toMap()

        val approveProposal = Sign.Params.Approve(
            proposerPublicKey = proposal.proposerPublicKey,
            namespaces = sessionNamespaces
        )

        SignClient.approveSession(approveProposal) { error ->
            Log.e(TAG, error.throwable.stackTraceToString())
        }
    }

    fun reject(proposal: Sign.Model.SessionProposal) {
        val reject = Sign.Params.Reject(
            proposerPublicKey = proposal.proposerPublicKey,
            reason = "Rejected by user"
        )

        SignClient.rejectSession(reject) { error ->
            Log.e(TAG, "reject onError: ", error.throwable)
        }
    }

    fun disconnect(topic: String) {
        val disconnect = Sign.Params.Disconnect(sessionTopic = topic)

        SignClient.disconnect(disconnect) { error ->
            Log.e(TAG, "disconnect onError: ", error.throwable)
            event = Event.Error(error.throwable)
        }

        sessionsUpdatedSubject.onNext(Unit)
    }

    fun respondPendingRequest(requestId: Long, topic: String, data: String) {
        val response = Sign.Params.Response(
            sessionTopic = topic,
            jsonRpcResponse = Sign.Model.JsonRpcResponse.JsonRpcResult(requestId, data)
        )

        SignClient.respond(response) {
            val error = it.throwable
            Log.e(TAG, "respondPendingRequest onError: ", error)
            event = Event.Error(error)
        }

        //todo remove delay after SDK has methods for updating requests after action (reject/respond)
        coroutineScope.launch {
            delay(1000L)
            pendingRequestUpdatedSubject.onNext(Unit)
        }
    }

    fun rejectRequest(topic: String, requestId: Long) {
        val response = Sign.Params.Response(
            sessionTopic = topic,
            jsonRpcResponse = Sign.Model.JsonRpcResponse.JsonRpcError(
                id = requestId,
                code = 500,
                message = "Rejected by user"
            )
        )

        SignClient.respond(response) {
            val error = it.throwable
            Log.e(TAG, "rejectRequest onError: ", error)
            event = Event.Error(error)
        }

        coroutineScope.launch {
            delay(1000L)
            pendingRequestUpdatedSubject.onNext(Unit)
        }
    }

    override fun onConnectionStateChange(state: Sign.Model.ConnectionState) {
        _connectionAvailableStateFlow.update {
            state.isAvailable
        }
    }

    override fun onError(error: Sign.Model.Error) {
        event = Event.Error(error.throwable)
    }

    override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) {
        event = Event.SessionDeleted(deletedSession)
        sessionsUpdatedSubject.onNext(Unit)
    }

    private val sessionProposals = CopyOnWriteArrayList<Sign.Model.SessionProposal>()

    fun getNextSessionProposal(): Sign.Model.SessionProposal? {
        return sessionProposals.removeFirstOrNull()
    }

    override fun onSessionProposal(sessionProposal: Sign.Model.SessionProposal, verifyContext: Sign.Model.VerifyContext) {
        sessionProposals.add(sessionProposal)
        event = Event.WaitingForApproveSession
    }

    override fun onSessionRequest(sessionRequest: Sign.Model.SessionRequest, verifyContext: Sign.Model.VerifyContext) {
        sessionsRequestReceivedSubject.onNext(sessionRequest)
        pendingRequestUpdatedSubject.onNext(Unit)
    }

    override fun onSessionSettleResponse(settleSessionResponse: Sign.Model.SettledSessionResponse) {
        when (settleSessionResponse) {
            is Sign.Model.SettledSessionResponse.Result -> {
                event = Event.SessionSettled(settleSessionResponse.session)
                sessionsUpdatedSubject.onNext(Unit)
            }
            is Sign.Model.SettledSessionResponse.Error -> {
                event = Event.Error(Throwable(settleSessionResponse.errorMessage))
            }
        }
    }

    override fun onSessionUpdateResponse(sessionUpdateResponse: Sign.Model.SessionUpdateResponse) {
//        TODO("Not yet implemented")
    }

}

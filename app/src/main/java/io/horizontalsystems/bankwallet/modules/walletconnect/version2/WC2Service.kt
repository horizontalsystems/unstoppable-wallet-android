package io.horizontalsystems.bankwallet.modules.walletconnect.version2

import android.util.Log
import com.walletconnect.walletconnectv2.client.WalletConnect
import com.walletconnect.walletconnectv2.client.WalletConnectClient
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WC2Service : WalletConnectClient.WalletDelegate {

    private val TAG = "WC2Service"

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val eventSubject = PublishSubject.create<Event>()
    val eventObservable: Flowable<Event>
        get() = eventSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val sessionsUpdatedSubject = PublishSubject.create<Unit>()
    val sessionsUpdatedObservable: Flowable<Unit>
        get() = sessionsUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val sessionsRequestReceivedSubject =
        PublishSubject.create<WalletConnect.Model.SessionRequest>()
    val sessionsRequestReceivedObservable: Flowable<WalletConnect.Model.SessionRequest>
        get() = sessionsRequestReceivedSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val pendingRequestUpdatedSubject = PublishSubject.create<Unit>()
    val pendingRequestUpdatedObservable: Flowable<Unit>
        get() = pendingRequestUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    val activeSessions: List<WalletConnect.Model.SettledSession>
//        get() = WalletConnectClient.getListOfSettledSessions()
        get() = listOf()

    init {
//        WalletConnectClient.setWalletDelegate(this)
    }

    fun pendingRequests(topic: String): List<WalletConnect.Model.JsonRpcHistory.HistoryEntry> {
        return listOf()
//        val history = WalletConnectClient.getJsonRpcHistory(topic)
//        return history.listOfRequests.filter { it.jsonRpcStatus == JsonRpcStatus.PENDING && it.method == "wc_sessionPayload" }
    }

    var event: Event = Event.Default
        private set(value) {
            field = value
            eventSubject.onNext(value)
        }

    sealed class Event {
        object Default : Event()
        class Error(val error: Throwable) : Event()
        class WaitingForApproveSession(val proposal: WalletConnect.Model.SessionProposal) : Event()
        class SessionSettled(val session: WalletConnect.Model.SettledSession) : Event()
        class SessionDeleted(val deletedSession: WalletConnect.Model.DeletedSession) : Event()
        object Ready : Event()
    }

    fun start() {
        sessionsUpdatedSubject.onNext(Unit)
    }

    fun pair(uri: String) {
        val pair = WalletConnect.Params.Pair(uri.trim())
        WalletConnectClient.pair(pair, object : WalletConnect.Listeners.Pairing {
            override fun onSuccess(settledPairing: WalletConnect.Model.SettledPairing) {}

            override fun onError(error: Throwable) {
                Log.e(TAG, "pair onError: ", error)
                event = Event.Error(error)
            }
        })
    }

    fun approve(proposal: WalletConnect.Model.SessionProposal, accounts: List<String>) {
        val approve = WalletConnect.Params.Approve(proposal, accounts)

        WalletConnectClient.approve(approve, object : WalletConnect.Listeners.SessionApprove {
            override fun onSuccess(settledSession: WalletConnect.Model.SettledSession) {
                event = Event.SessionSettled(settledSession)
                sessionsUpdatedSubject.onNext(Unit)
            }

            override fun onError(error: Throwable) {
                Log.e(TAG, "approve onError: ", error)
                event = Event.Error(error)
            }
        })
    }

    fun reject(proposal: WalletConnect.Model.SessionProposal) {
        val proposalTopic: String = proposal.topic
        val reject = WalletConnect.Params.Reject("Reject Session", proposalTopic)

        WalletConnectClient.reject(reject, object : WalletConnect.Listeners.SessionReject {
            override fun onSuccess(rejectedSession: WalletConnect.Model.RejectedSession) {}

            override fun onError(error: Throwable) {
                Log.e(TAG, "reject onError: ", error)
                event = Event.Error(error)
            }
        })
    }

    fun disconnect(topic: String) {
        val disconnect = WalletConnect.Params.Disconnect(
            sessionTopic = topic,
            reason = "User disconnected session",
            reasonCode = 1000
        )

        WalletConnectClient.disconnect(disconnect, object : WalletConnect.Listeners.SessionDelete {
            override fun onSuccess(deletedSession: WalletConnect.Model.DeletedSession) {
                event = Event.SessionDeleted(deletedSession)
                sessionsUpdatedSubject.onNext(Unit)
            }

            override fun onError(error: Throwable) {
                Log.e(TAG, "disconnect onError: ", error)
                event = Event.Error(error)
            }
        })

        sessionsUpdatedSubject.onNext(Unit)
    }

    fun respondPendingRequest(requestId: Long, topic: String, data: String) {
        val response = WalletConnect.Params.Response(
            sessionTopic = topic,
            jsonRpcResponse = WalletConnect.Model.JsonRpcResponse.JsonRpcResult(requestId, data)
        )

        WalletConnectClient.respond(response, object : WalletConnect.Listeners.SessionPayload {
            override fun onError(error: Throwable) {
                Log.e(TAG, "respondPendingRequest onError: ", error)
                event = Event.Error(error)
            }
        })

        //todo remove delay after SDK has methods for updating requests after action (reject/respond)
        coroutineScope.launch {
            delay(1000L)
            pendingRequestUpdatedSubject.onNext(Unit)
        }
    }

    fun rejectRequest(topic: String, requestId: Long) {
        val response = WalletConnect.Params.Response(
            sessionTopic = topic,
            jsonRpcResponse = WalletConnect.Model.JsonRpcResponse.JsonRpcError(
                requestId,
                WalletConnect.Model.JsonRpcResponse.Error(500, "Unstoppable Wallet Error")
            )
        )

        WalletConnectClient.respond(response, object : WalletConnect.Listeners.SessionPayload {
            override fun onError(error: Throwable) {
                Log.e(TAG, "rejectRequest onError: ", error)
                event = Event.Error(error)
            }

        })

        coroutineScope.launch {
            delay(1000L)
            pendingRequestUpdatedSubject.onNext(Unit)
        }
    }

    override fun onSessionProposal(sessionProposal: WalletConnect.Model.SessionProposal) {
        event = Event.WaitingForApproveSession(sessionProposal)
    }

    override fun onSessionRequest(sessionRequest: WalletConnect.Model.SessionRequest) {
        sessionsRequestReceivedSubject.onNext(sessionRequest)
        pendingRequestUpdatedSubject.onNext(Unit)
    }

    override fun onSessionDelete(deletedSession: WalletConnect.Model.DeletedSession) {
        event = Event.SessionDeleted(deletedSession)
        sessionsUpdatedSubject.onNext(Unit)
    }

    override fun onSessionNotification(sessionNotification: WalletConnect.Model.SessionNotification) {
        // Triggered when the peer emits events as notifications that match the list of types agreed upon session settlement
    }

}

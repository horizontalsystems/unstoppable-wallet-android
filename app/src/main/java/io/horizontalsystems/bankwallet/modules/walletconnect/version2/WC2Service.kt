package io.horizontalsystems.bankwallet.modules.walletconnect.version2

import android.util.Log
import com.walletconnect.walletconnectv2.client.WalletConnect
import com.walletconnect.walletconnectv2.client.WalletConnectClient
import com.walletconnect.walletconnectv2.storage.history.model.JsonRpcStatus
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject

class WC2Service : WalletConnectClient.WalletDelegate {

    private val TAG = "WC2Service"

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
        get() = WalletConnectClient.getListOfSettledSessions()

    fun pendingRequests(topic: String): List<WalletConnect.Model.JsonRpcHistory.HistoryEntry> {
        val history = WalletConnectClient.getJsonRpcHistory(topic)
        return history.listOfRequests.filter { it.jsonRpcStatus == JsonRpcStatus.PENDING && it.method == "wc_sessionPayload"}
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

    init {
        WalletConnectClient.setWalletDelegate(this)
    }

    fun start() {
        sessionsUpdatedSubject.onNext(Unit)
    }

    fun stop() {

    }

    fun pair(uri: String) {
        Log.e(TAG, "pair: $uri")
        val pair = WalletConnect.Params.Pair(uri.trim())
        WalletConnectClient.pair(pair, object : WalletConnect.Listeners.Pairing {
            override fun onSuccess(settledPairing: WalletConnect.Model.SettledPairing) {
                Log.e(TAG, "onSuccess settledPairing: ${settledPairing.topic}")
            }

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
                Log.e(
                    TAG,
                    "approve success topic: ${settledSession.topic} accounts: ${settledSession.accounts}"
                )
                event = Event.SessionSettled(settledSession)
                sessionsUpdatedSubject.onNext(Unit)
            }

            override fun onError(error: Throwable) {
                Log.e(TAG, "onError: ", error)
                event = Event.Error(error)
            }
        })
    }

    fun reject(proposal: WalletConnect.Model.SessionProposal) {
        val rejectionReason = "Reject Session"
        val proposalTopic: String = proposal.topic
        val reject = WalletConnect.Params.Reject(rejectionReason, proposalTopic)

        WalletConnectClient.reject(reject, object : WalletConnect.Listeners.SessionReject {
            override fun onSuccess(rejectedSession: WalletConnect.Model.RejectedSession) {
//                stateSubject.onNext(RejectSession)
                Log.e(TAG, "reject success: ")
            }

            override fun onError(error: Throwable) {
                Log.e(TAG, "onError: ", error)
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
                Log.e(TAG, "session disconnected: $topic")
            }

            override fun onError(error: Throwable) {
                Log.e(TAG, "onError: ", error)
                event = Event.Error(error)
            }
        })

        sessionsUpdatedSubject.onNext(Unit)
    }

    fun respondPendingRequest(requestId: Long, topic: String, data: String){
        val response = WalletConnect.Params.Response(
            sessionTopic = topic,
            jsonRpcResponse = WalletConnect.Model.JsonRpcResponse.JsonRpcResult(requestId, data)
        )

        WalletConnectClient.respond(response, object : WalletConnect.Listeners.SessionPayload {
            override fun onError(error: Throwable) {
                Log.e(TAG, "onError: ", error)
                event = Event.Error(error)
            }
        })

        pendingRequestUpdatedSubject.onNext(Unit)
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
                Log.e(TAG, "onError: ", error)
                event = Event.Error(error)
            }

        })

        pendingRequestUpdatedSubject.onNext(Unit)
    }

    fun sessionUpdate(session: WalletConnect.Model.SettledSession) {
//        val proposalNonNull = proposal ?: return
//
//        val update = WalletConnect.Params.Update(
//            sessionTopic = session.topic,
//            sessionState = WalletConnect.Model.SessionState(accounts = listOf("${proposalNonNull.chains[0]}:0xa0A6c118b1B25207A8A764E1CAe1635339bedE62"))
//        )
//
//        WalletConnectClient.update(update, object : WalletConnect.Listeners.SessionUpdate {
//            override fun onSuccess(updatedSession: WalletConnect.Model.UpdatedSession) {
////                stateSubject.onNext(
////                    UpdateActiveSessions(
////                        WalletConnectClient.getListOfSettledSessions(),
////                        "Successful session update"
////                    )
////                )
//            }
//
//            override fun onError(error: Throwable) {
//                //Error
//                Log.e(TAG, "onError: ", error)
//            }
//        })
    }

    fun sessionUpgrade(session: WalletConnect.Model.SettledSession) {
        val permissions =
            WalletConnect.Model.SessionPermissions(
                blockchain = WalletConnect.Model.Blockchain(chains = listOf("eip155:80001")),
                jsonRpc = WalletConnect.Model.Jsonrpc(listOf("eth_sign"))
            )
        val upgrade = WalletConnect.Params.Upgrade(topic = session.topic, permissions = permissions)

        WalletConnectClient.upgrade(upgrade, object : WalletConnect.Listeners.SessionUpgrade {
            override fun onSuccess(upgradedSession: WalletConnect.Model.UpgradedSession) {
//                stateSubject.onNext(
//                    UpdateActiveSessions(
//                        WalletConnectClient.getListOfSettledSessions(),
//                        "Successful session upgrade"
//                    )
//                )
            }

            override fun onError(error: Throwable) {
                //Error
                Log.e(TAG, "onError: ", error)
            }
        })
    }

    override fun onSessionProposal(sessionProposal: WalletConnect.Model.SessionProposal) {
        Log.e(TAG, "onSessionProposal: ${sessionProposal.topic}")
        event = Event.WaitingForApproveSession(sessionProposal)
    }

    override fun onSessionRequest(sessionRequest: WalletConnect.Model.SessionRequest) {
        Log.e(TAG, "onSessionRequest topic: ${sessionRequest.topic}")
        sessionsRequestReceivedSubject.onNext(sessionRequest)
        pendingRequestUpdatedSubject.onNext(Unit)
    }

    override fun onSessionDelete(deletedSession: WalletConnect.Model.DeletedSession) {
        Log.e(TAG, "onSessionDelete topic: ${deletedSession.topic}")
        event = Event.SessionDeleted(deletedSession)
        sessionsUpdatedSubject.onNext(Unit)
    }

    override fun onSessionNotification(sessionNotification: WalletConnect.Model.SessionNotification) {
        // Triggered when the peer emits events as notifications that match the list of types agreed upon session settlement
        Log.e(TAG, "onSessionNotification: ${sessionNotification.topic}")
    }

}

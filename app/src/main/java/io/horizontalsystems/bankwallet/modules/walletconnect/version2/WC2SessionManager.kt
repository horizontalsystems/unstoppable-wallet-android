package io.horizontalsystems.bankwallet.modules.walletconnect.version2

import android.util.Log
import com.walletconnect.walletconnectv2.client.WalletConnect
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.walletconnect.entity.WalletConnectV2Session
import io.horizontalsystems.bankwallet.modules.walletconnect.storage.WC2SessionStorage
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class WC2SessionManager(
    private val accountManager: IAccountManager,
    private val storage: WC2SessionStorage,
    private val service: WC2Service
) {

    private val TAG = "WC2SessionManager"

    private val disposable = CompositeDisposable()
    private val sessionsSubject = PublishSubject.create<List<WalletConnect.Model.SettledSession>>()
    val sessionsObservable: Flowable<List<WalletConnect.Model.SettledSession>>
        get() = sessionsSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val pendingRequestsSubject = PublishSubject.create<List<WalletConnect.Model.JsonRpcHistory.HistoryEntry>>()
    val pendingRequestsObservable: Flowable<List<WalletConnect.Model.JsonRpcHistory.HistoryEntry>>
        get() = pendingRequestsSubject.toFlowable(BackpressureStrategy.BUFFER)

    val sessions: List<WalletConnect.Model.SettledSession>
        get() {
            val accountId = accountManager.activeAccount?.id ?: return emptyList()
            return getSessions(accountId)
        }

    val allSessions: List<WalletConnect.Model.SettledSession>
        get() = service.activeSessions

    init {
        service.start()
        syncSessions()

        accountManager.activeAccountObservable
            .subscribeIO {
                syncSessions()
            }
            .let { disposable.add(it) }

        accountManager.accountsDeletedFlowable
            .subscribeIO {
                handleDeletedAccount()
            }
            .let { disposable.add(it) }

        service.sessionsUpdatedObservable
            .subscribeIO {
                syncSessions()
            }
            .let { disposable.add(it) }

        service.pendingRequestUpdatedObservable
            .subscribeIO {
                syncPendingRequest()
            }
            .let { disposable.add(it) }

        service.sessionsRequestReceivedObservable
            .subscribeIO {
                handleSessionRequest(it)
            }
            .let { disposable.add(it) }
    }

    fun deleteSession(topic: String) {
        service.disconnect(topic)
    }

    fun pendingRequests(accountId: String? = null): List<WalletConnect.Model.JsonRpcHistory.HistoryEntry>{
        return requests(accountId)
    }

    private fun syncSessions() {
        val accountId = accountManager.activeAccount?.id ?: return

        val currentSessions = allSessions
        Log.e(TAG, "syncSessions: ${currentSessions.size}")
        val allDbSessions = storage.getSessionsByAccountId(accountId)

        val dbTopics = allDbSessions.map { it.topic }

        val newSessions = currentSessions.filterNot { dbTopics.contains(it.topic) }
        val deletedTopics = dbTopics.filter { topic ->
            !currentSessions.any { it.topic == topic }
        }

        currentSessions.forEach { Log.e(TAG, "currentSessions: ${it.topic}") }
        dbTopics.forEach { Log.e(TAG, "dbTopics: ${it}") }
        deletedTopics.forEach { Log.e(TAG, "deletedTopics: ${it}") }

        storage.save(newSessions.map { WalletConnectV2Session(accountId, it.topic) })
        storage.deleteSessionsByTopics(deletedTopics)

        sessionsSubject.onNext(getSessions(accountId))
        syncPendingRequest()
    }

    private fun handleSessionRequest(request: WalletConnect.Model.SessionRequest) {
//        sessions
//
//        guard activeSessions.first(where: { session in session.topic == request.topic }) != nil,
//        let request = try? WalletConnectV2RequestMapper.map(request: request) else {
//            return
//        }
//
//            sessionRequestReceivedRelay.accept(request)
    }

    private fun getSessions(accountId: String): List<WalletConnect.Model.SettledSession> {
        val sessions = service.activeSessions
        val dbSessions = storage.getSessionsByAccountId(accountId)

        val accountSessions = sessions.filter { session ->
            dbSessions.any { it.topic == session.topic }
        }

        return accountSessions
    }

    private fun syncPendingRequest() {
        pendingRequestsSubject.onNext(requests())
    }

    private fun requests(accountId: String? = null): List<WalletConnect.Model.JsonRpcHistory.HistoryEntry> {
        val sessions = accountId?.let { getSessions(it) } ?: allSessions
        val pendingRequests = mutableListOf<WalletConnect.Model.JsonRpcHistory.HistoryEntry>()
        sessions.forEach { session ->
            pendingRequests.addAll(service.pendingRequests(session.topic))
        }
        return pendingRequests
    }

    private fun handleDeletedAccount() {
        val existingAccountIds = accountManager.accounts.map { it.id }
        storage.deleteSessionsExcept(accountIds = existingAccountIds)

        syncSessions()
    }

}

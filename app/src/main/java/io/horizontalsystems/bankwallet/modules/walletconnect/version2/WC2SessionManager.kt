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

    val sessions: List<WalletConnect.Model.SettledSession>
        get() {
            val accountId = accountManager.activeAccount?.id ?: return emptyList()
            return getSessions(accountId)
        }

    val allSessions: List<WalletConnect.Model.SettledSession>
        get() = service.activeSessions

    init {
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

        service.sessionsRequestReceivedObservable
            .subscribeIO {
                handleSessionRequest(it)
            }
            .let { disposable.add(it) }
    }

    private fun handleDeletedAccount() {
        val existingAccountIds = accountManager.accounts.map { it.id }
        storage.deleteSessionsExcept(accountIds = existingAccountIds)

        syncSessions()
    }

    private fun syncSessions() {
        val accountId = accountManager.activeAccount?.id ?: return

        val currentSessions = allSessions
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
//        pendingRequestsRelay.accept(requests())
    }

    fun deleteSession(topic: String) {
        service.disconnect(topic)
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

    private fun requests(accountId: String? = null): List<WalletConnect.Model.SessionRequest> {
//        val allRequests = service.pendingRequests
//        val dbSessions = storage.sessionsV2(accountId: accountId)
//
//        return allRequests.filter { request in
//                dbSessions.contains { session in
//                        session.topic == request.topic
//                }
//        }
        return emptyList()
    }

}

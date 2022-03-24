package io.horizontalsystems.bankwallet.modules.walletconnect.version2

import android.util.Log
import com.walletconnect.walletconnectv2.client.WalletConnect
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
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
    val service: WC2Service,
    private val wcManager: WC2Manager
) {

    private val TAG = "WC2SessionManager"

    var pendingRequestDataToOpen = mutableMapOf<Long, RequestData>()

    private val disposable = CompositeDisposable()
    private val sessionsSubject = PublishSubject.create<List<WalletConnect.Model.SettledSession>>()
    val sessionsObservable: Flowable<List<WalletConnect.Model.SettledSession>>
        get() = sessionsSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val pendingRequestCountSubject = PublishSubject.create<Int>()
    val pendingRequestCountObservable: Flowable<Int>
        get() = pendingRequestCountSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val pendingRequestSubject = PublishSubject.create<WC2Request>()
    val pendingRequestObservable: Flowable<WC2Request>
        get() = pendingRequestSubject.toFlowable(BackpressureStrategy.BUFFER)

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

    fun pendingRequests(accountId: String? = null): List<WalletConnect.Model.JsonRpcHistory.HistoryEntry> {
        return requests(accountId)
    }

    fun sessionByTopic(topic: String): WalletConnect.Model.SettledSession? {
        return allSessions.firstOrNull { it.topic == topic }
    }

    fun deleteSession(sessionId: String) {
        service.disconnect(sessionId)
    }

    fun prepareRequestToOpen(requestId: Long) {
        val account = accountManager.activeAccount ?: throw RequestDataError.NoSuitableAccount
        val request = requests(account.id)
            .firstOrNull { it.requestId == requestId }
            ?: throw RequestDataError.RequestNotFoundError
        val chainId =
            WC2Parser.getChainIdFromBody(request.body) ?: throw RequestDataError.UnsupportedChainId
        val evmKitWrapper =
            wcManager.getEvmKitWrapper(chainId, account) ?: throw RequestDataError.NoSuitableEvmKit
        val dAppName = sessionByTopic(request.topic)?.peerAppMetaData?.name ?: ""
        val receiveAddress = evmKitWrapper.evmKit.receiveAddress.eip55
        val transactionRequest =
            WC2Parser.parseTransactionRequest(request, receiveAddress, dAppName)
                ?: throw RequestDataError.DataParsingError
        pendingRequestDataToOpen[requestId] = RequestData(transactionRequest, evmKitWrapper)
    }

    private fun syncSessions() {
        val accountId = accountManager.activeAccount?.id ?: return

        val currentSessions = allSessions

        val allDbSessions = storage.getAllSessions()
        val allDbTopics = allDbSessions.map { it.topic }

        val newSessions = currentSessions.filter { !allDbTopics.contains(it.topic) }
        val deletedTopics = allDbTopics.filter { topic ->
            !currentSessions.any { it.topic == topic }
        }

        storage.save(newSessions.map { WalletConnectV2Session(accountId, it.topic) })
        storage.deleteSessionsByTopics(deletedTopics)

        sessionsSubject.onNext(getSessions(accountId))
        syncPendingRequest()
    }

    private fun handleSessionRequest(sessionRequest: WalletConnect.Model.SessionRequest) {
        try {
            prepareRequestToOpen(sessionRequest.request.id)
        } catch (error: Throwable) {
            Log.e(TAG, "handleSessionRequest error: ", error)
        }

        pendingRequestDataToOpen[sessionRequest.request.id]?.let {
            pendingRequestSubject.onNext(it.pendingRequest)
        }
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
        pendingRequestCountSubject.onNext(requests().size)
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

    data class RequestData(
        val pendingRequest: WC2Request,
        val evmKitWrapper: EvmKitWrapper
    )

    open class RequestDataError : Throwable() {
        object UnsupportedChainId : RequestDataError()
        object NoSuitableAccount : RequestDataError()
        object NoSuitableEvmKit : RequestDataError()
        object DataParsingError : RequestDataError()
        object RequestNotFoundError : RequestDataError()
    }

}

package io.horizontalsystems.bankwallet.modules.walletconnect.version2

import com.walletconnect.sign.client.Sign
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.ActiveAccountState
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.walletconnect.entity.WalletConnectV2Session
import io.horizontalsystems.bankwallet.modules.walletconnect.storage.WC2SessionStorage
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WC2SessionManager(
    private val accountManager: IAccountManager,
    private val storage: WC2SessionStorage,
    val service: WC2Service,
    private val wcManager: WC2Manager
) {

    private val TAG = "WC2SessionManager"

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val disposable = CompositeDisposable()
    private val sessionsSubject = PublishSubject.create<List<Sign.Model.Session>>()
    val sessionsObservable: Flowable<List<Sign.Model.Session>>
        get() = sessionsSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val _pendingRequestCountFlow = MutableStateFlow(0)
    val pendingRequestCountFlow: StateFlow<Int>
        get() = _pendingRequestCountFlow

    private val pendingRequestSubject = PublishSubject.create<Long>()
    val pendingRequestObservable: Flowable<Long>
        get() = pendingRequestSubject.toFlowable(BackpressureStrategy.BUFFER)

    val sessions: List<Sign.Model.Session>
        get() {
            val accountId = accountManager.activeAccount?.id ?: return emptyList()
            return getSessions(accountId)
        }

    val allSessions: List<Sign.Model.Session>
        get() = service.activeSessions

    init {
        service.start()
        syncSessions()

        coroutineScope.launch {
            accountManager.activeAccountStateFlow.collect { activeAccountState ->
                if (activeAccountState is ActiveAccountState.ActiveAccount) {
                    syncSessions()
                }
            }
        }

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

    fun sessionByTopic(topic: String): Sign.Model.Session? {
        return allSessions.firstOrNull { it.topic == topic }
    }

    fun deleteSession(sessionId: String) {
        service.disconnect(sessionId)
    }

    fun createRequestData(requestId: Long): RequestData {
        val account = accountManager.activeAccount ?: throw RequestDataError.NoSuitableAccount
        val request = requests(account.id)
            .firstOrNull { it.requestId == requestId }
            ?: throw RequestDataError.RequestNotFoundError
        val chainId = request.chainId?.split(":")?.last()?.toInt() ?: throw RequestDataError.UnsupportedChainId
        val evmKitWrapper = wcManager.getEvmKitWrapper(chainId, account) ?: throw RequestDataError.NoSuitableEvmKit
        val dAppName = sessionByTopic(request.topic)?.metaData?.name ?: ""
        val receiveAddress = evmKitWrapper.evmKit.receiveAddress.eip55
        val wc2Request = WC2Parser.parseRequest(request, receiveAddress, dAppName)

        return RequestData(wc2Request, evmKitWrapper)
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

    private fun handleSessionRequest(sessionRequest: Sign.Model.SessionRequest) {
        if (sessions.any { it.topic == sessionRequest.topic }) {
            pendingRequestSubject.onNext(sessionRequest.request.id)
        }
    }

    private fun getSessions(accountId: String): List<Sign.Model.Session> {
        val sessions = service.activeSessions
        val dbSessions = storage.getSessionsByAccountId(accountId)

        val accountSessions = sessions.filter { session ->
            dbSessions.any { it.topic == session.topic }
        }

        return accountSessions
    }

    private fun syncPendingRequest() {
        val requestsCount = accountManager.activeAccount?.let { requests(it.id).size } ?: 0
        _pendingRequestCountFlow.update { requestsCount }
    }

    private fun requests(accountId: String): List<Sign.Model.PendingRequest> {
        val sessions = getSessions(accountId)
        val pendingRequests = mutableListOf<Sign.Model.PendingRequest>()
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
        object RequestNotFoundError : RequestDataError()
    }

}

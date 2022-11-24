package io.horizontalsystems.bankwallet.modules.walletconnect.requestlist

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Parser
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Request
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WC2RequestListService(
    private val sessionManager: WC2SessionManager,
    private val accountManager: IAccountManager
) {

    private val disposables = CompositeDisposable()
    private var accounts: List<Account> = listOf()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val itemsSubject = PublishSubject.create<List<WC2RequestListModule.Item>>()
    val itemsObservable: Flowable<List<WC2RequestListModule.Item>>
        get() = itemsSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val pendingRequestSubject = PublishSubject.create<WC2Request>()
    val pendingRequestObservable: Flowable<WC2Request>
        get() = pendingRequestSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val errorSubject = PublishSubject.create<String>()
    val errorObservable: Flowable<String>
        get() = errorSubject.toFlowable(BackpressureStrategy.BUFFER)

    var items: List<WC2RequestListModule.Item> = listOf()
        set(value) {
            field = value
            itemsSubject.onNext(value)
        }

    fun start() {
        accountManager.accountsFlowable
            .subscribeIO {
                syncAccounts(it)
            }.let { disposables.add(it) }

        accountManager.activeAccountObservable
            .subscribeIO {
                syncPendingRequests()
            }.let { disposables.add(it) }

        coroutineScope.launch {
            sessionManager.pendingRequestCountFlow.collect {
                syncPendingRequests()
            }
        }

        syncAccounts(accountManager.accounts)
        syncPendingRequests()
    }

    fun stop() {
        disposables.clear()
        coroutineScope.cancel()
    }

    fun select(accountId: String) {
        accountManager.setActiveAccountId(accountId)
    }

    fun onRequestClick(requestId: Long) {
        try {
            sessionManager.prepareRequestToOpen(requestId)
            sessionManager.pendingRequestDataToOpen[requestId]?.let { requestData ->
                pendingRequestSubject.onNext(requestData.pendingRequest)
            }
        } catch (error: Throwable){
            errorSubject.onNext(error.message ?: error.javaClass.simpleName)
        }
    }

    private fun syncAccounts(accounts: List<Account>) {
        val activeAccountId = accountManager.activeAccount?.id ?: return

        val currentAccountIds = this.accounts.map { it.id }.toSet()
        val accountIds = accounts.map { it.id }.toSet()

        if (currentAccountIds != accountIds) {
            this.accounts = accounts.sortedByDescending { it.id == activeAccountId }
        }

        syncPendingRequests()
    }

    private fun syncPendingRequests() {
        val activeAccountId = accountManager.activeAccount?.id ?: return

        val itemList = mutableListOf<WC2RequestListModule.Item>()
        val allSessions = sessionManager.allSessions

        accounts.forEach { account ->
            val pendingRequests = sessionManager.pendingRequests(account.id)

            if (pendingRequests.isNotEmpty()) {
                itemList.add(
                    WC2RequestListModule.Item(
                        account.id,
                        account.name,
                        account.id == activeAccountId,
                        pendingRequests.map { request ->
                            val appMetaData =
                                allSessions.firstOrNull { it.topic == request.topic }?.metaData

                            val chainName = request.chainId?.let {
                                WC2Parser.getAccountData(it)
                            }?.chain?.name ?: ""

                            WC2RequestListModule.RequestItem(
                                id = request.requestId,
                                chainName = chainName,
                                method = request.method,
                                imageUrl = appMetaData?.icons?.lastOrNull()
                            )
                        }
                    )
                )
            }
        }

        items = itemList
    }
}

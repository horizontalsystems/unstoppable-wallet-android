package io.horizontalsystems.bankwallet.modules.walletconnect.requestlist

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Parser
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class WC2RequestListService(
    private val sessionManager: WC2SessionManager,
    private val accountManager: IAccountManager
) {

    private val disposables = CompositeDisposable()
    private var accounts: List<Account> = listOf()

    private val itemsSubject = PublishSubject.create<List<WC2RequestListModule.Item>>()
    val itemsObservable: Flowable<List<WC2RequestListModule.Item>>
        get() = itemsSubject.toFlowable(BackpressureStrategy.BUFFER)

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

        sessionManager.pendingRequestsObservable
            .subscribeIO {
                syncPendingRequests()
            }.let { disposables.add(it) }

        syncAccounts(accountManager.accounts)
        syncPendingRequests()
    }

    fun stop() {
        disposables.clear()
    }

    fun select(accountId: String) {
        accountManager.setActiveAccountId(accountId)
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
                            WC2RequestListModule.RequestItem(
                                id = request.requestId,
                                sessionName = allSessions.firstOrNull { it.topic == request.topic }?.peerAppMetaData?.name
                                    ?: "",
                                method = WC2Parser.getSessionRequestMethod(request.body),
                                chainId = null
                            )
                        }
                    )
                )
            }
        }

        items = itemList
    }
}

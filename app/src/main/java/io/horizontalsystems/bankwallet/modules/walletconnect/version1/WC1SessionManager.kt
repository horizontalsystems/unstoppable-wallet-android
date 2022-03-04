package io.horizontalsystems.bankwallet.modules.walletconnect.version1

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.AccountSettingManager
import io.horizontalsystems.bankwallet.modules.walletconnect.storage.WC1SessionStorage
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.walletconnect.entity.WalletConnectSession
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class WC1SessionManager(
    private val storage: WC1SessionStorage,
    private val accountManager: IAccountManager,
    private val accountSettingManager: AccountSettingManager
) {
    private val disposable = CompositeDisposable()

    private val sessionsSubject = PublishSubject.create<List<WalletConnectSession>>()
    val sessionsObservable: Flowable<List<WalletConnectSession>>
        get() = sessionsSubject.toFlowable(BackpressureStrategy.BUFFER)

    val sessions: List<WalletConnectSession>
        get() = accountManager.activeAccount?.let { account ->
            val ethereumChainId = accountSettingManager.ethereumNetwork(account).chain.id
            val binanceSmartChainChainId = accountSettingManager.binanceSmartChainNetwork(account).chain.id

            storage.getSessions(account.id, listOf(ethereumChainId, binanceSmartChainChainId))
        } ?: listOf()

    init {
        accountManager.accountsDeletedFlowable
                .subscribeIO {
                    handleDeletedAccounts()
                }
                .let {
                    disposable.add(it)
                }

        accountManager.activeAccountObservable
                .subscribeIO {
                    handleActiveAccount(it.orElse(null))
                }
                .let {
                    disposable.add(it)
                }

        accountSettingManager.ethereumNetworkObservable
            .subscribeIO {
                syncSessions()
            }
            .let {
                disposable.add(it)
            }

        accountSettingManager.binanceSmartChainNetworkObservable
            .subscribeIO {
                syncSessions()
            }
            .let {
                disposable.add(it)
            }
    }

    private fun handleActiveAccount(account: Account?) {
        syncSessions()
    }

    fun save(session: WalletConnectSession) {
        storage.save(session)
        syncSessions()
    }

    fun deleteSession(peerId: String) {
        storage.deleteSessionsByPeerId(peerId)
        syncSessions()
    }

    private fun handleDeletedAccounts() {
        val existingAccountIds = accountManager.accounts.map { it.id }
        storage.deleteSessionsExcept(accountIds = existingAccountIds)

        syncSessions()
    }

    private fun syncSessions() {
        sessionsSubject.onNext(sessions)
    }

}

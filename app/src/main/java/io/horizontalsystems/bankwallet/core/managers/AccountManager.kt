package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAccountsStorage
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject

class AccountManager(private val storage: IAccountsStorage) : IAccountManager {

    private val cache = AccountsCache()
    private val accountsSubject = PublishSubject.create<List<Account>>()

    override val accounts: List<Account>
        get() = cache.accountsSet.toList()

    override val accountsFlowable: Flowable<List<Account>>
        get() = accountsSubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun preloadAccounts() {
        cache.set(storage.allAccounts())
    }

    override fun save(account: Account) {
        cache.add(account)

        storage.save(account)
        accountsSubject.onNext(accounts)
    }

    override fun delete(id: String) {
        cache.delete(id)
        storage.delete(id)

        accountsSubject.onNext(accounts)
    }

    private class AccountsCache {
        var accountsSet = mutableSetOf<Account>()
            private set

        fun add(account: Account) {
            accountsSet.add(account)
        }

        fun set(accounts: List<Account>) {
            accountsSet = accounts.toMutableSet()
        }

        fun delete(id: String) {
            accountsSet.removeAll { it.id == id }
        }
    }
}

package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAccountsStorage
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.CoinType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject

class AccountManager(private val storage: IAccountsStorage) : IAccountManager {

    private val cache = AccountsCache()
    private val accountsSubject = PublishSubject.create<List<Account>>()
    private val deleteAccountSubject = PublishSubject.create<String>()

    override val isAccountsEmpty: Boolean
        get() = storage.isAccountsEmpty

    override val accounts: List<Account>
        get() = cache.accountsSet.toList()

    override val accountsFlowable: Flowable<List<Account>>
        get() = accountsSubject.toFlowable(BackpressureStrategy.BUFFER)
    override val deleteAccountObservable: Flowable<String>
        get() = deleteAccountSubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun account(coinType: CoinType): Account? {
        return accounts.find { account -> coinType.canSupport(account.type) }
    }

    override fun preloadAccounts() {
        cache.set(storage.allAccounts())
    }

    override fun create(account: Account) {
        storage.save(account)

        cache.insert(account)
        accountsSubject.onNext(accounts)
    }

    override fun update(account: Account) {
        storage.save(account)

        cache.update(account)
        accountsSubject.onNext(accounts)
    }

    override fun delete(id: String) {
        cache.delete(id)
        storage.delete(id)

        accountsSubject.onNext(accounts)
        deleteAccountSubject.onNext(id)
    }

    override fun clear() {
        storage.clear()
        cache.set(listOf())

        accountsSubject.onNext(accounts)
    }

    private class AccountsCache {
        var accountsSet = mutableSetOf<Account>()
            private set

        fun insert(account: Account) {
            accountsSet.add(account)
        }

        fun update(account: Account) {
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

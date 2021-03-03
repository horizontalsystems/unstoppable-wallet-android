package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountCleaner
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAccountsStorage
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.canSupport
import io.horizontalsystems.coinkit.models.CoinType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class AccountManager(private val storage: IAccountsStorage, private val accountCleaner: IAccountCleaner) : IAccountManager {

    private val cache = AccountsCache()
    private val accountsSubject = PublishSubject.create<List<Account>>()
    private val accountsDeletedSubject = PublishSubject.create<Unit>()

    override val isAccountsEmpty: Boolean
        get() = storage.isAccountsEmpty

    override val accounts: List<Account>
        get() = cache.accountsSet.toList()

    override val accountsFlowable: Flowable<List<Account>>
        get() = accountsSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val accountsDeletedFlowable: Flowable<Unit>
        get() = accountsDeletedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun account(coinType: CoinType): Account? {
        return accounts.find { account -> coinType.canSupport(account.type) }
    }

    override fun loadAccounts() {
        val accounts = storage.allAccounts()
        cache.set(accounts)
    }

    override fun save(account: Account) {
        storage.save(account)

        cache.insert(account)
        accountsSubject.onNext(accounts)
    }

    override fun update(account: Account) {
        storage.update(account)

        cache.update(account)
        accountsSubject.onNext(accounts)
    }

    override fun delete(id: String) {
        cache.delete(id)
        storage.delete(id)

        accountsSubject.onNext(accounts)
        accountsDeletedSubject.onNext(Unit)
    }

    override fun clear() {
        storage.clear()
        cache.set(listOf())
        accountsSubject.onNext(listOf())
        accountsDeletedSubject.onNext(Unit)
    }

    override fun clearAccounts() {
        val clearAsync = Single.fromCallable {
            accountCleaner.clearAccounts(storage.getDeletedAccountIds())
            storage.clearDeleted()
        }

        Single.timer(3, TimeUnit.SECONDS)
                .flatMap { clearAsync }
                .subscribeOn(Schedulers.io())
                .subscribe()
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

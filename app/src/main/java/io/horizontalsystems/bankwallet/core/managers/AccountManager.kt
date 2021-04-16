package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountCleaner
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAccountsStorage
import io.horizontalsystems.bankwallet.entities.Account
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.*
import java.util.concurrent.TimeUnit

class AccountManager(
        private val storage: IAccountsStorage,
        private val accountCleaner: IAccountCleaner
) : IAccountManager {

    private val cache = AccountsCache()
    private val accountsSubject = PublishSubject.create<List<Account>>()
    private val accountsDeletedSubject = PublishSubject.create<Unit>()
    private val activeAccountSubject = PublishSubject.create<Optional<Account>>()

    override val activeAccount: Account?
        get() = cache.activeAccount

    override val activeAccountObservable: Flowable<Optional<Account>>
        get() = activeAccountSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val isAccountsEmpty: Boolean
        get() = storage.isAccountsEmpty

    override val accounts: List<Account>
        get() = cache.accountsSet.toList()

    override val accountsFlowable: Flowable<List<Account>>
        get() = accountsSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val accountsDeletedFlowable: Flowable<Unit>
        get() = accountsDeletedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun setActiveAccountId(activeAccountId: String?) {
        if (cache.activeAccount?.id != activeAccountId) {
            storage.activeAccountId = activeAccountId
            cache.setActiveAccountId(activeAccountId)
            activeAccountSubject.onNext(Optional.ofNullable(activeAccount))
        }
    }

    override fun account(id: String): Account? {
        return accounts.find { account -> account.id == id }
    }

    override fun loadAccounts() {
        cache.set(storage.allAccounts())
        cache.setActiveAccountId(storage.activeAccountId)
    }

    override fun save(account: Account) {
        storage.save(account)

        cache.insert(account)
        accountsSubject.onNext(accounts)

        setActiveAccountId(account.id)
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

        if (id == activeAccount?.id) {
            setActiveAccountId(accounts.firstOrNull()?.id)
        }
    }

    override fun clear() {
        storage.clear()
        cache.set(listOf())
        accountsSubject.onNext(listOf())
        accountsDeletedSubject.onNext(Unit)
        setActiveAccountId(null)
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

        var activeAccount: Account? = null
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

        fun setActiveAccountId(activeAccountId: String?) {
            activeAccount = if (activeAccountId != null) {
                accountsSet.find { it.id == activeAccountId}
            } else {
                null
            }
        }
    }
}

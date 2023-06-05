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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Optional
import java.util.concurrent.TimeUnit

class AccountManager(
        private val storage: IAccountsStorage,
        private val accountCleaner: IAccountCleaner
) : IAccountManager {

    private val cache = AccountsCache()
    private val accountsSubject = PublishSubject.create<List<Account>>()
    private val accountsDeletedSubject = PublishSubject.create<Unit>()
    private val activeAccountSubject = PublishSubject.create<Optional<Account>>()
    private val _activeAccountStateFlow = MutableStateFlow<ActiveAccountState>(ActiveAccountState.NotLoaded)

    override val activeAccountStateFlow = _activeAccountStateFlow

    override val hasNonStandardAccount: Boolean
        get() = cache.accountsMap.any { it.value.nonStandard }

    override val activeAccount: Account?
        get() = cache.activeAccount

    override val activeAccountObservable: Flowable<Optional<Account>>
        get() = activeAccountSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val isAccountsEmpty: Boolean
        get() = storage.isAccountsEmpty

    override val accounts: List<Account>
        get() = cache.accountsMap.map { it.value }

    override val accountsFlowable: Flowable<List<Account>>
        get() = accountsSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val accountsDeletedFlowable: Flowable<Unit>
        get() = accountsDeletedSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val _newAccountBackupRequiredFlow = MutableStateFlow<Account?>(null)
    override val newAccountBackupRequiredFlow = _newAccountBackupRequiredFlow.asStateFlow()

    override fun setActiveAccountId(activeAccountId: String?) {
        if (cache.activeAccountId != activeAccountId) {
            storage.activeAccountId = activeAccountId
            cache.activeAccountId = activeAccountId
            activeAccountSubject.onNext(Optional.ofNullable(activeAccount))
            _activeAccountStateFlow.update { ActiveAccountState.ActiveAccount(activeAccount) }
        }
    }

    override fun account(id: String): Account? {
        return accounts.find { account -> account.id == id }
    }

    override fun loadAccounts() {
        cache.set(storage.allAccounts())
        cache.activeAccountId = storage.activeAccountId
        activeAccountSubject.onNext(Optional.ofNullable(activeAccount))
        _activeAccountStateFlow.update { ActiveAccountState.ActiveAccount(activeAccount) }
    }

    override fun onHandledBackupRequiredNewAccount() {
        _newAccountBackupRequiredFlow.update { null }
    }

    override fun save(account: Account) {
        storage.save(account)

        cache.set(account)
        accountsSubject.onNext(accounts)

        setActiveAccountId(account.id)
        if (!account.isBackedUp && !account.isFileBackedUp) {
            _newAccountBackupRequiredFlow.update {
                account
            }
        }
    }

    override fun update(account: Account) {
        storage.update(account)

        cache.set(account)
        accountsSubject.onNext(accounts)

        activeAccount?.id?.let {
            if (account.id == it) {
                activeAccountSubject.onNext(Optional.ofNullable(activeAccount))
                _activeAccountStateFlow.update { ActiveAccountState.ActiveAccount(activeAccount) }
            }
        }
    }

    override fun delete(id: String) {
        cache.delete(id)
        storage.delete(id)

        accountsSubject.onNext(accounts)
        accountsDeletedSubject.onNext(Unit)

        if (id == cache.activeAccountId) {
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
        var activeAccountId: String? = null

        var accountsMap = mutableMapOf<String, Account>()
            private set

        val activeAccount: Account?
            get() = activeAccountId?.let { accountsMap[it] }

        fun set(account: Account) {
            accountsMap[account.id] = account
        }

        fun set(accounts: List<Account>) {
            accountsMap = accounts.associateBy { it.id }.toMutableMap()
        }

        fun delete(id: String) {
            accountsMap.remove(id)
        }
    }
}

class NoActiveAccount : Exception()

sealed class ActiveAccountState() {
    class ActiveAccount(val account: Account?) : ActiveAccountState()
    object NotLoaded : ActiveAccountState()
}
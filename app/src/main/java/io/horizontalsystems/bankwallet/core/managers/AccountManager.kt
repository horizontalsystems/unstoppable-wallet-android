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

    private var activeAccountId: String? = null
    private var accountsCache = mutableMapOf<String, Account>()
    private val accountsSubject = PublishSubject.create<List<Account>>()
    private val accountsDeletedSubject = PublishSubject.create<Unit>()
    private val activeAccountSubject = PublishSubject.create<Optional<Account>>()
    private val _activeAccountStateFlow = MutableStateFlow<ActiveAccountState>(ActiveAccountState.NotLoaded)
    private var accountsMinLevel = Int.MAX_VALUE

    override val activeAccountStateFlow = _activeAccountStateFlow

    override val hasNonStandardAccount: Boolean
        get() = accountsCache.any { it.value.nonStandard }

    override val activeAccount: Account?
        get() = activeAccountId?.let { accountsCache[it] }

    override val activeAccountObservable: Flowable<Optional<Account>>
        get() = activeAccountSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val isAccountsEmpty: Boolean
        get() = storage.isAccountsEmpty

    override val accounts: List<Account>
        get() = accountsCache.map { it.value }

    override val accountsFlowable: Flowable<List<Account>>
        get() = accountsSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val accountsDeletedFlowable: Flowable<Unit>
        get() = accountsDeletedSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val _newAccountBackupRequiredFlow = MutableStateFlow<Account?>(null)
    override val newAccountBackupRequiredFlow = _newAccountBackupRequiredFlow.asStateFlow()

    private fun updateCache(account: Account) {
        accountsCache[account.id] = account
    }

    override fun setActiveAccountId(activeAccountId: String?) {
        if (this.activeAccountId != activeAccountId) {
            storage.activeAccountId = activeAccountId
            this.activeAccountId = activeAccountId
            activeAccountSubject.onNext(Optional.ofNullable(activeAccount))
            _activeAccountStateFlow.update { ActiveAccountState.ActiveAccount(activeAccount) }
        }
    }

    override fun account(id: String): Account? {
        return accounts.find { account -> account.id == id }
    }

    override fun loadAccounts() {
        refreshCache()
        activeAccountId = storage.activeAccountId
        activeAccountSubject.onNext(Optional.ofNullable(activeAccount))
        _activeAccountStateFlow.update { ActiveAccountState.ActiveAccount(activeAccount) }
    }

    private fun refreshCache() {
        accountsCache = storage.allAccounts(accountsMinLevel).associateBy { it.id }.toMutableMap()
    }

    override fun onHandledBackupRequiredNewAccount() {
        _newAccountBackupRequiredFlow.update { null }
    }

    override fun save(account: Account) {
        storage.save(account)

        updateCache(account)
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

        updateCache(account)
        accountsSubject.onNext(accounts)

        activeAccount?.id?.let {
            if (account.id == it) {
                activeAccountSubject.onNext(Optional.ofNullable(activeAccount))
                _activeAccountStateFlow.update { ActiveAccountState.ActiveAccount(activeAccount) }
            }
        }
    }

    override fun delete(id: String) {
        accountsCache.remove(id)
        storage.delete(id)

        accountsSubject.onNext(accounts)
        accountsDeletedSubject.onNext(Unit)

        if (id == activeAccountId) {
            setActiveAccountId(accounts.firstOrNull()?.id)
        }
    }

    override fun clear() {
        storage.clear()
        accountsCache.clear()
        accountsSubject.onNext(listOf())
        accountsDeletedSubject.onNext(Unit)
        setActiveAccountId(null)
    }

    override fun setLevel(level: Int) {
        // if the same level
        if (level == accountsMinLevel) return

        accountsMinLevel = level
        refreshCache()

        accountsSubject.onNext(accounts)

        // if there was no active account
        val tmpActiveAccountId = activeAccountId ?: return

        // if the active account is available for new level
        if (accountsCache.containsKey(tmpActiveAccountId)) return

        setActiveAccountId(accountsCache.values.firstOrNull()?.id)
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

}

class NoActiveAccount : Exception()

sealed class ActiveAccountState() {
    class ActiveAccount(val account: Account?) : ActiveAccountState()
    object NotLoaded : ActiveAccountState()
}
package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAccountCleaner
import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.core.IAccountsStorage
import io.horizontalsystems.walletkit.entities.Account
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountManager(
    private val storage: IAccountsStorage,
    private val accountCleaner: IAccountCleaner
) : IAccountManager {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var accountsCache = mutableMapOf<String, Account>()
    private val _accountsFlow = MutableSharedFlow<List<Account>>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _accountsDeletedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _activeAccountStateFlow = MutableStateFlow<ActiveAccountState>(ActiveAccountState.NotLoaded)
    private var currentLevel = Int.MAX_VALUE

    override val activeAccountStateFlow = _activeAccountStateFlow

    override val hasNonStandardAccount: Boolean
        get() = accountsCache.any { it.value.nonStandard }

    override var activeAccount: Account? = null

    override val isAccountsEmpty: Boolean
        get() = storage.isAccountsEmpty

    override val accounts: List<Account>
        get() = accountsCache.map { it.value }

    override val accountsFlow: Flow<List<Account>>
        get() = _accountsFlow

    override val accountsDeletedFlow: Flow<Unit>
        get() = _accountsDeletedFlow

    private fun updateCache(account: Account) {
        accountsCache[account.id] = account
    }

    override fun setActiveAccountId(activeAccountId: String?) {
        if (activeAccount?.id != activeAccountId) {
            storage.setActiveAccountId(currentLevel, activeAccountId)
            activeAccount = activeAccountId?.let { account(it) }
            _activeAccountStateFlow.update {
                ActiveAccountState.ActiveAccount(activeAccount)
            }
        }
    }

    override fun account(id: String): Account? {
        return accounts.find { account -> account.id == id }
    }

    override fun save(account: Account) {
        storage.save(account)

        updateCache(account)
        _accountsFlow.tryEmit(accounts)

        setActiveAccountId(account.id)
    }

    override fun import(accounts: List<Account>) {
        for (account in accounts) {
            storage.save(account)
            updateCache(account)
        }

        _accountsFlow.tryEmit(accounts)

        if (activeAccount == null) {
            accounts.minByOrNull { it.name.lowercase() }?.let { account ->
                setActiveAccountId(account.id)
            }
        }
    }

    override fun updateAccountLevels(accountIds: List<String>, level: Int) {
        storage.updateLevels(accountIds, level)
    }

    override fun updateMaxLevel(level: Int) {
        storage.updateMaxLevel(level)
    }

    override fun update(account: Account) {
        storage.update(account)

        updateCache(account)
        _accountsFlow.tryEmit(accounts)

        activeAccount?.id?.let {
            if (account.id == it) {
                activeAccount = account
                _activeAccountStateFlow.update { ActiveAccountState.ActiveAccount(activeAccount) }
            }
        }
    }

    override fun delete(id: String) {
        accountsCache.remove(id)
        storage.delete(id)

        _accountsFlow.tryEmit(accounts)
        _accountsDeletedFlow.tryEmit(Unit)

        if (id == activeAccount?.id) {
            setActiveAccountId(accounts.firstOrNull()?.id)
        }
    }

    override fun clear() {
        storage.clear()
        accountsCache.clear()
        _accountsFlow.tryEmit(listOf())
        _accountsDeletedFlow.tryEmit(Unit)
        setActiveAccountId(null)
    }

    override fun setLevel(level: Int) {
        currentLevel = level

        accountsCache = storage.allAccounts(level).associateBy { it.id }.toMutableMap()

        val activeAccountIdForLevel = storage.getActiveAccountId(level)
        if (activeAccount == null || activeAccount?.id != activeAccountIdForLevel) {
            activeAccount = accountsCache[activeAccountIdForLevel] ?: accounts.firstOrNull()
            _activeAccountStateFlow.update {
                ActiveAccountState.ActiveAccount(activeAccount)
            }
        }

        _accountsFlow.tryEmit(accounts)
    }

    override fun clearAccounts() {
        coroutineScope.launch {
            delay(3000)
            accountCleaner.clearAccounts(storage.getDeletedAccountIds())
            storage.clearDeleted()
        }
    }

    override fun getRandomWalletName(): String {
        val existingNames = accounts.map { it.name }.toSet()
        val all = App.instance.localizedContext().resources.getStringArray(R.array.wallet_names)
        return all.filter { it !in existingNames }.randomOrNull() ?: all.random()
    }

}

class NoActiveAccount : Exception()

sealed class ActiveAccountState() {
    class ActiveAccount(val account: Account?) : ActiveAccountState()
    object NotLoaded : ActiveAccountState()
}
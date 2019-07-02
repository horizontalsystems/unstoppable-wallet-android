package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.Account
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ISecuredStorage
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject

class AccountManager(private val secureStorage: ISecuredStorage) : IAccountManager {

    private val accountsSubject = PublishSubject.create<List<Account>>()
    private val nonBackedUpCountSubject = PublishSubject.create<Int>()

    private val accountsMap: MutableMap<String, Account>
        get() = secureStorage.accounts.associateBy { it.id }.toMutableMap()

    // IAccountManager methods

    override val accounts: List<Account>
        get() = secureStorage.accounts

    override val accountsFlowable: Flowable<List<Account>>
        get() = accountsSubject.toFlowable(BackpressureStrategy.LATEST)

    override val nonBackedUpCount: Int
        get() = accounts.filter { !it.isBackedUp }.size

    override val nonBackedUpCountFlowable: Flowable<Int>
        get() = nonBackedUpCountSubject.toFlowable(BackpressureStrategy.LATEST)

    override fun save(account: Account) {
        val accounts = accountsMap
        accounts[account.id] = account

        saveAccounts(accounts)
    }

    override fun delete(id: String) {
        val accounts = accountsMap
        accounts.remove(id)

        saveAccounts(accounts)
    }

    override fun setIsBackedUp(id: String) {
        val accounts = accountsMap
        accounts[id]?.isBackedUp = true

        saveAccounts(accounts)
    }

    private fun saveAccounts(accountsMap: Map<String, Account>) {
        val accountsList = accountsMap.map { it.value }

        secureStorage.saveAccounts(accountsList)

        accountsSubject.onNext(accountsList)
        nonBackedUpCountSubject.onNext(nonBackedUpCount)
    }

}

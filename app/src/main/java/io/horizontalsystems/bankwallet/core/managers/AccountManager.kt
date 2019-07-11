package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.Account
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAccountsStorage
import io.horizontalsystems.bankwallet.core.IPredefinedAccountType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject

class AccountManager(private val accountsStorage: IAccountsStorage) : IAccountManager {

    val nonBackedUpCount: Int
        get() = accounts.filter { !it.isBackedUp }.size

    override var accounts = accountsStorage.getAll().toMutableList()
        private set

    private val accountsSubject = PublishSubject.create<List<Account>>()
    private val nonBackedUpCountSubject = PublishSubject.create<Int>()

    override val accountsFlowable: Flowable<List<Account>>
        get() = accountsSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val nonBackedUpCountFlowable: Flowable<Int>
        get() = nonBackedUpCountSubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun account(predefinedAccountType: IPredefinedAccountType): Account? {
        return accounts.find { predefinedAccountType.supports(it.type) }
    }

    override fun save(account: Account) {
        accounts.removeAll { it.id == account.id }
        accounts.add(account)

        accountsStorage.save(account)
        notifyAccountsChanged()
    }

    override fun delete(id: String) {
        accounts.removeAll { it.id == id }
        accountsStorage.delete(id)

        notifyAccountsChanged()
    }

    override fun setIsBackedUp(id: String) {
        accounts.find { it.id == id }?.let { account ->
            account.isBackedUp = true
        }

        accountsStorage.setIsBackedUp(id)
        notifyAccountsChanged()
    }

    private fun notifyAccountsChanged() {
        accountsSubject.onNext(accounts)
        nonBackedUpCountSubject.onNext(nonBackedUpCount)
    }
}

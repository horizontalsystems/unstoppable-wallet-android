package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.Account
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAccountsStorage
import io.reactivex.Flowable

class AccountManager(private val accountsStorage: IAccountsStorage) : IAccountManager {

    // IAccountManager methods

    override val accountsFlowable: Flowable<List<Account>>
        get() = accountsStorage.getAll()

    override val nonBackedUpCountFlowable: Flowable<Int>
        get() = accountsStorage.getNonBackedUpCount()

    override fun save(account: Account) {
        accountsStorage.save(account)
    }

    override fun delete(id: String) {
        accountsStorage.delete(id)
    }

    override fun setIsBackedUp(id: String) {
        accountsStorage.setIsBackedUp(id)
    }

}

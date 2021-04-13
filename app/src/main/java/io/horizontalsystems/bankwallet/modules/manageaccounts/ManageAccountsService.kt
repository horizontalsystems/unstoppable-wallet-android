package io.horizontalsystems.bankwallet.modules.manageaccounts

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.entities.Account
import io.reactivex.Flowable

class ManageAccountsService(
        private val accountManager: IAccountManager
) {

    val accounts: List<Account>
        get() = accountManager.accounts

    val accountsObservable: Flowable<List<Account>>
        get() = accountManager.accountsFlowable

}

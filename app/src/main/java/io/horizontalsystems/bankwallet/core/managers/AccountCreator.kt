package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.factories.AccountFactory
import io.horizontalsystems.bankwallet.entities.SyncMode

class AccountCreator(private val accountManager: IAccountManager, private val accountFactory: AccountFactory)
    : IAccountCreator {

    override fun createRestoredAccount(accountType: AccountType, syncMode: SyncMode?): Account {
        val account = accountFactory.account(accountType, true, syncMode ?: SyncMode.FAST)
        accountManager.save(account)
        return account
    }

    override fun createNewAccount(defaultAccountType: DefaultAccountType): Account {
        TODO("not implemented")
    }
}

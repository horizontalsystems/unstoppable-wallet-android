package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.Account
import io.horizontalsystems.bankwallet.core.AccountType
import io.horizontalsystems.bankwallet.core.IAccountCreator
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.factories.AccountFactory
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.entities.SyncMode

class AccountCreator(private val accountManager: IAccountManager, private val accountFactory: AccountFactory)
    : IAccountCreator {

    override fun createRestoredAccount(accountType: AccountType, syncMode: SyncMode): Account {
        val account = accountFactory.account(accountType, backedUp = true, defaultSyncMode = syncMode)
        accountManager.save(account)
        return account
    }

    override fun createNewAccount(type: PredefinedAccountType): Account {
        TODO("not implemented")
    }
}

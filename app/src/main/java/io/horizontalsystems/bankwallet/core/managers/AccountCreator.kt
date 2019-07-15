package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountCreator
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.SyncMode

class AccountCreator(private val accountManager: IAccountManager, private val accountFactory: IAccountFactory)
    : IAccountCreator {

    override fun createRestoredAccount(accountType: AccountType, syncMode: SyncMode?): Account {
        return createAccount(accountType, isBackedUp = true, defaultSyncMode = syncMode)
    }

    override fun createNewAccount(accountType: AccountType): Account {
        return createAccount(accountType, isBackedUp = false, defaultSyncMode = SyncMode.NEW)
    }

    private fun createAccount(accountType: AccountType, isBackedUp: Boolean, defaultSyncMode: SyncMode?): Account {
        val syncMode = defaultSyncMode ?: SyncMode.FAST
        val account = accountFactory.account(accountType, isBackedUp, syncMode)

        accountManager.save(account)
        return account
    }
}

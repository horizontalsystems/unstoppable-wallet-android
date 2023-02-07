package cash.p.terminal.core.factories

import cash.p.terminal.core.IAccountFactory
import cash.p.terminal.core.IAccountManager
import cash.p.terminal.entities.Account
import cash.p.terminal.entities.AccountOrigin
import cash.p.terminal.entities.AccountType
import java.util.*

class AccountFactory(val accountManager: IAccountManager) : IAccountFactory {

    override fun account(name: String, type: AccountType, origin: AccountOrigin, backedUp: Boolean): Account {
        val id = UUID.randomUUID().toString()

        return Account(
                id = id,
                name = name,
                type = type,
                origin = origin,
                isBackedUp = backedUp
        )
    }

    override fun watchAccount(name: String, type: AccountType): Account {
        val id = UUID.randomUUID().toString()
        return Account(
            id = id,
            name = name,
            type = type,
            origin = AccountOrigin.Restored,
            isBackedUp = true
        )
    }

    override fun getNextWatchAccountName(): String {
        val watchAccountsCount = accountManager.accounts.count { it.isWatchAccount }

        return "Watch Wallet ${watchAccountsCount + 1}"
    }

    override fun getNextAccountName(): String {
        val nonWatchAccountsCount = accountManager.accounts.count { !it.isWatchAccount }

        return "Wallet ${nonWatchAccountsCount + 1}"
    }
}

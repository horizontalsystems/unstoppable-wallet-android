package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import java.util.*

class AccountFactory(val accountManager: IAccountManager) : IAccountFactory {

    override fun account(type: AccountType, origin: AccountOrigin, backedUp: Boolean): Account {
        val id = UUID.randomUUID().toString()

        return Account(
                id = id,
                name = getNextAccountName(),
                type = type,
                origin = origin,
                isBackedUp = backedUp
        )
    }

    override fun watchAccount(address: String, domain: String?): Account {
        val id = UUID.randomUUID().toString()
        return Account(
            id = id,
            name = domain ?: getNextWatchAccountName(),
            type = AccountType.Address(address),
            origin = AccountOrigin.Restored,
            isBackedUp = true
        )
    }

    private fun getNextWatchAccountName(): String {
        val watchAccountsCount = accountManager.accounts.count {
            it.type is AccountType.Address
        }

        return "Watch Wallet ${watchAccountsCount + 1}"
    }

    private fun getNextAccountName(): String {
        val nonWatchAccountsCount = accountManager.accounts.count {
            it.type !is AccountType.Address
        }

        return "Wallet ${nonWatchAccountsCount + 1}"
    }
}

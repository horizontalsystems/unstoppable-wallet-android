package cash.p.terminal.core.factories

import cash.p.terminal.core.IAccountFactory
import cash.p.terminal.core.managers.UserManager
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.CexType
import cash.p.terminal.wallet.IAccountManager
import java.util.UUID

class AccountFactory(
    private val accountManager: IAccountManager,
    private val userManager: UserManager
) : IAccountFactory {

    override fun account(
        name: String,
        type: AccountType,
        origin: AccountOrigin,
        backedUp: Boolean,
        fileBackedUp: Boolean
    ): Account {
        val id = UUID.randomUUID().toString()

        return Account(
            id = id,
            name = name,
            type = type,
            origin = origin,
            level = userManager.getUserLevel(),
            isBackedUp = backedUp,
            isFileBackedUp = fileBackedUp
        )
    }

    override fun watchAccount(name: String, type: AccountType): Account {
        val id = UUID.randomUUID().toString()
        return Account(
            id = id,
            name = name,
            type = type,
            origin = AccountOrigin.Restored,
            level = userManager.getUserLevel(),
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

    override fun getNextCexAccountName(cexType: CexType): String {
        val cexAccountsCount = accountManager.accounts.count {
            it.type is AccountType.Cex && cexType.sameType((it.type as AccountType.Cex).cexType) }

        return "${cexType.name()} Wallet ${cexAccountsCount + 1}"
    }
}

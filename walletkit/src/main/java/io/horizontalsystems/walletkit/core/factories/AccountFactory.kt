package io.horizontalsystems.walletkit.core.factories

import androidx.annotation.StringRes
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.IAccountFactory
import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.core.managers.UserManager
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountOrigin
import io.horizontalsystems.walletkit.entities.AccountType
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
        return nextUnusedName(R.string.WalletName_WatchDefault)
    }

    override fun getNextAccountName(): String {
        return nextUnusedName(R.string.WalletName_Default)
    }

    private fun nextUnusedName(@StringRes template: Int): String {
        val existingNames = accountManager.accounts.map { it.name }.toSet()

        return firstUnusedName(existingNames) { Translator.getString(template, it) }
    }

}

/** The lowest-numbered name, counting from 1, that is not in [existingNames]. */
internal fun firstUnusedName(existingNames: Set<String>, name: (Int) -> String): String {
    // existingNames.size + 1 numbers always hold an unused one, as long as each number gives its own name
    return (1..existingNames.size + 1).firstNotNullOfOrNull { number ->
        name(number).takeIf { it !in existingNames }
    } ?: name(1)
}

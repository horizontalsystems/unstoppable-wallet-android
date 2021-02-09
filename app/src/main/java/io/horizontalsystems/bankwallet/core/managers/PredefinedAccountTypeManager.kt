package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountCreator
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IPredefinedAccountTypeManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType

class PredefinedAccountTypeManager(private val accountManager: IAccountManager, private val accountCreator: IAccountCreator)
    : IPredefinedAccountTypeManager {

    override val allTypes: List<PredefinedAccountType>
        get() = listOf(PredefinedAccountType.Standard, PredefinedAccountType.Binance, PredefinedAccountType.Zcash)

    override fun account(predefinedAccountType: PredefinedAccountType): Account? {
        return accountManager.accounts.find { predefinedAccountType.supports(it.type) }
    }

    override fun predefinedAccountType(type: AccountType): PredefinedAccountType? {
        return allTypes.firstOrNull { it.supports(type) }
    }
}

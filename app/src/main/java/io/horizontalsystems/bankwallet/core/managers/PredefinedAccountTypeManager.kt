package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType

class PredefinedAccountTypeManager(private val appConfigProvider: IAppConfigProvider, private val accountManager: IAccountManager, private val accountCreator: IAccountCreator)
    : IPredefinedAccountTypeManager {

    override val allTypes: List<IPredefinedAccountType>
        get() = appConfigProvider.predefinedAccountTypes

    override fun account(predefinedAccountType: IPredefinedAccountType): Account? {
        return accountManager.accounts.find { predefinedAccountType.supports(it.type) }
    }

    override fun createAccount(predefinedAccountType: IPredefinedAccountType): Account? {
        return accountCreator.createNewAccount(predefinedAccountType.defaultAccountType, createDefaultWallets = true)
    }

    override fun predefinedAccountType(type: AccountType): IPredefinedAccountType? {
        return allTypes.firstOrNull { it.supports(type) }
    }
}

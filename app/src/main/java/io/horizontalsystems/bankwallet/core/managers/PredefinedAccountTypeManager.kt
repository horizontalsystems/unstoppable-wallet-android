package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Account

class PredefinedAccountTypeManager(private val appConfigProvider: IAppConfigProvider, private val accountManager: IAccountManager, private val accountCreator: IAccountCreator)
    : IPredefinedAccountTypeManager {

    override val allTypes: List<IPredefinedAccountType>
        get() = appConfigProvider.predefinedAccountTypes

    override fun account(predefinedAccountType: IPredefinedAccountType): Account? {
        return accountManager.accounts.find { predefinedAccountType.supports(it.type) }
    }

    override fun createAccount(predefinedAccountType: IPredefinedAccountType): Account? {
        return accountCreator.createNewAccount(predefinedAccountType.defaultAccountType, enabledDefaults = true)
    }

    override fun createAllAccounts() {
        allTypes.forEach { predefinedAccountType ->
            createAccount(predefinedAccountType)
        }
    }
}

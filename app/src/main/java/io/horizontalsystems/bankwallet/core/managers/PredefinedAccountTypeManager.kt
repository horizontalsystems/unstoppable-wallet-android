package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Account

class PredefinedAccountTypeManager(val appConfigProvider: IAppConfigProvider, val accountManager: IAccountManager)
    : IPredefinedAccountTypeManager {

    override val allTypes: List<IPredefinedAccountType>
        get() = appConfigProvider.predefinedAccountTypes

    override fun account(predefinedAccountType: IPredefinedAccountType): Account? {
        return accountManager.accounts.find { predefinedAccountType.supports(it.type) }
    }
}

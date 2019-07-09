package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IPredefinedAccountType
import io.horizontalsystems.bankwallet.core.IPredefinedAccountTypeManager

class PredefinedAccountTypeManager(val appConfigProvider: IAppConfigProvider, val accountManager: IAccountManager)
    : IPredefinedAccountTypeManager {

    override val allTypes: List<IPredefinedAccountType>
        get() = appConfigProvider.predefinedAccountTypes
}

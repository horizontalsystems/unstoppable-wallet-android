package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ILaunchManager

class AppManager(private val accountManager: IAccountManager) : ILaunchManager {
    override fun onStart() {
        accountManager.preloadAccounts()
    }
}

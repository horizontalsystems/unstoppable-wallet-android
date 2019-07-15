package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ILaunchManager
import io.horizontalsystems.bankwallet.core.IWalletManager

class AppManager(private val accountManager: IAccountManager, private val walletManager: IWalletManager) : ILaunchManager {
    override fun onStart() {
        accountManager.preloadAccounts()
        walletManager.preloadWallets()
    }
}

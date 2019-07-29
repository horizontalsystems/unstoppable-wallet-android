package io.horizontalsystems.bankwallet.modules.main

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager

class MainInteractor(private val accountManager: IAccountManager,
                     private val walletManager: IWalletManager) : MainModule.IInteractor {

    var delegate: MainModule.IInteractorDelegate? = null

    override fun onStart() {
        accountManager.preloadAccounts()
        walletManager.preloadWallets()
    }

}

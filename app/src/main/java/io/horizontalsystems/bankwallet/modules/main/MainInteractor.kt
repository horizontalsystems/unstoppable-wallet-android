package io.horizontalsystems.bankwallet.modules.main

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IWalletManager

class MainInteractor(private val accountManager: IAccountManager, private val walletManager: IWalletManager, private val adapterManager: IAdapterManager)
    : MainModule.IInteractor {

    var delegate: MainModule.IInteractorDelegate? = null

    override fun onStart() {
        accountManager.loadAccounts()
        walletManager.loadWallets()
        adapterManager.preloadAdapters()

        accountManager.clearAccounts()
    }
}

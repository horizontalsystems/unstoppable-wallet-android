package io.horizontalsystems.bankwallet.modules.main

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IRateAppManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.reactivex.disposables.CompositeDisposable

class MainInteractor(
        private val accountManager: IAccountManager,
        private val walletManager: IWalletManager,
        private val adapterManager: IAdapterManager,
        private val rateAppManager: IRateAppManager)
    : MainModule.IInteractor {

    var delegate: MainModule.IInteractorDelegate? = null
    val disposables = CompositeDisposable()

    override fun onStart() {
        rateAppManager.onAppLaunch()
        accountManager.loadAccounts()
        walletManager.loadWallets()
        adapterManager.preloadAdapters()
        accountManager.clearAccounts()
        rateAppManager.onAppBecomeActive()

        rateAppManager.showRateAppObservable
                .subscribe {
                    delegate?.didShowRateApp()
                }
                .let {
                    disposables.add(it)
                }
    }

    override fun clear() {
        disposables.clear()
    }
}

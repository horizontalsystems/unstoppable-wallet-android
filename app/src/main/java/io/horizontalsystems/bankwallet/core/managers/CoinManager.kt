package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.IWordsManager
import io.reactivex.disposables.CompositeDisposable

class CoinManager(private val wordsManager: IWordsManager,
                  private val walletManager: IWalletManager,
                  private val appConfigProvider: IAppConfigProvider) {

    private var disposables: CompositeDisposable = CompositeDisposable()

    init {
        wordsManager.loggedInSubject.subscribe {
            syncWallets()
        }.let {
            disposables.add(it)
        }
    }

    private fun syncWallets() {
        wordsManager.words?.let {
            walletManager.initWallets(it, appConfigProvider.enabledCoins)
        } ?: walletManager.clearWallets()
    }

}

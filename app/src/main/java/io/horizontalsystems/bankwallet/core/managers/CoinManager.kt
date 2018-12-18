package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.IWordsManager
import io.horizontalsystems.bankwallet.core.LogInState
import io.horizontalsystems.bankwallet.entities.*
import io.reactivex.disposables.CompositeDisposable

class CoinManager(private val wordsManager: IWordsManager,
                  private val walletManager: IWalletManager,
                  private val appConfigProvider: IAppConfigProvider) {

    private var disposables: CompositeDisposable = CompositeDisposable()

    init {
        wordsManager.loggedInSubject.subscribe { logInState ->
            syncWallets(logInState == LogInState.CREATE)
        }.let {
            disposables.add(it)
        }
    }

    private fun syncWallets(newWallet: Boolean) {
        wordsManager.words?.let {
            walletManager.initWallets(it, defaultCoins, newWallet)
        } ?: walletManager.clearWallets()
    }

    private val defaultCoins: List<Coin>
        get() {
            var suffix = ""
            if (appConfigProvider.network == Network.TEST) {
                suffix = "t"
            }
            val coins = mutableListOf<Coin>()
            coins.add(Coin("Bitcoin", "BTC$suffix", blockChain = BlockChain.Bitcoin(BitcoinType.Bitcoin)))
            coins.add(Coin("Bitcoin Cash", "BCH$suffix", blockChain = BlockChain.Bitcoin(BitcoinType.BitcoinCash)))
            coins.add(Coin("Ethereum", "ETH$suffix", blockChain = BlockChain.Ethereum(EthereumType.Ethereum)))
            return coins
        }

}

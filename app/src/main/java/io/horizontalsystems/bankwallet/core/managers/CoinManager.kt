package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType

class CoinManager(private val appConfigProvider: IAppConfigProvider) {

    val coins = defaultCoins

    private val defaultCoins: List<Coin>
        get() {
            val suffix = if (appConfigProvider.testMode) "t" else ""
            val coins = mutableListOf<Coin>()
            coins.add(Coin("Bitcoin", "BTC$suffix", true, CoinType.Bitcoin, 0))
            coins.add(Coin("Bitcoin Cash", "BCH$suffix", true, CoinType.BitcoinCash, 1))
            coins.add(Coin("Ethereum", "ETH$suffix", true, CoinType.Ethereum, 2))
            return coins
        }

}

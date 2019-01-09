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
            coins.add(Coin("Bitcoin", "BTC$suffix", CoinType.Bitcoin))
            coins.add(Coin("Bitcoin Cash", "BCH$suffix", CoinType.BitcoinCash))
            coins.add(Coin("Ethereum", "ETH$suffix", CoinType.Ethereum))
            return coins
        }

}

package io.horizontalsystems.bankwallet.core.providers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType

class FeeCoinProvider(private val appConfigProvider: IAppConfigProvider) {

    fun feeCoinData(coin: Coin): Pair<Coin, String>? = when (coin.type) {
        is CoinType.Erc20 -> erc20()
        is CoinType.Bep20 -> bep20()
        is CoinType.Binance -> binance(coin.type.symbol)
        else -> null
    }

    private fun erc20(): Pair<Coin, String> {
        val coin = appConfigProvider.ethereumCoin

        return Pair(coin, "ERC20")
    }

    private fun bep20(): Pair<Coin, String> {
        val coin = appConfigProvider.binanceSmartChainCoin

        return Pair(coin, "BEP20")
    }

    private fun binance(symbol: String): Pair<Coin, String>? {
        if (symbol == "BNB") {
            return null
        }
        val coin = appConfigProvider.binanceCoin

        return Pair(coin, "BEP2")
    }

}

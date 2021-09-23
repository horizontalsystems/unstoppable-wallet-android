package io.horizontalsystems.bankwallet.core.providers

import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin

class FeeCoinProvider(private val marketKit: MarketKit) {

    fun feeCoinData(coinType: CoinType): Pair<PlatformCoin, String>? = when (coinType) {
        is CoinType.Erc20 -> erc20()
        is CoinType.Bep20 -> bep20()
        is CoinType.Bep2 -> binance(coinType.symbol)
        else -> null
    }

    private fun erc20(): Pair<PlatformCoin, String>? {
        return marketKit.platformCoin(CoinType.Ethereum)?.let {
            Pair(it, "ERC20")
        }
    }

    private fun bep20(): Pair<PlatformCoin, String>? {
        return marketKit.platformCoin(CoinType.BinanceSmartChain)?.let {
            Pair(it, "BEP20")
        }
    }

    private fun binance(symbol: String): Pair<PlatformCoin, String>? {
        if (symbol == "BNB") {
            return null
        }
        return marketKit.platformCoin(CoinType.Bep2("BNB"))?.let {
            Pair(it, "BEP2")
        }
    }

}

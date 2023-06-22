package io.horizontalsystems.bankwallet.modules.balance.cex

import io.horizontalsystems.bankwallet.core.customCoinPrefix
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.TokenQuery

class ConzixCexCoinMapper(marketKit: MarketKitWrapper) {
    private val coins = marketKit.allCoins().map { it.uid to it }.toMap()

    fun getCoin(currency: Response.Currency): Coin {
        return mapBinanceAssetToCoin[currency.iso3]
            ?.let { coinUid ->
                coins[coinUid]
            } ?: Coin(
            uid = "${TokenQuery.customCoinPrefix}${currency.iso3}",
            name = currency.name,
            code = currency.iso3
        )
    }

    companion object {
        private val mapBinanceAssetToCoin = mapOf(
            "USDT" to "tether",
            "BUSD" to "binance-usd",
            "AGIX" to "singularitynet",
            "SUSHI" to "sushi",
            "GMT" to "stepn",
            "CAKE" to "pancakeswap-token",
            "ETH" to "ethereum",
            "ETHW" to "ethereum-pow-iou",
            "BTC" to "bitcoin",
            "BNB" to "binancecoin",
            "SOL" to "solana",
            "QI" to "benqi",
            "BSW" to "biswap",
        )
    }


}

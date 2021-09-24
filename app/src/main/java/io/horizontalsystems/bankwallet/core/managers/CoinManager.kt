package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ICustomTokenStorage
import io.horizontalsystems.bankwallet.entities.CustomToken
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.MarketCoin
import io.horizontalsystems.marketkit.models.PlatformCoin

class CoinManager(
    private val marketKit: MarketKit,
    private val storage: ICustomTokenStorage
) : ICoinManager {
    private val featuredCoinTypes: List<CoinType> = listOf(CoinType.Bitcoin, CoinType.Ethereum, CoinType.BinanceSmartChain)

    override fun featuredMarketCoins(enabledCoinTypes: List<CoinType>): List<MarketCoin> {
        val appMarketCoins = customMarketCoins(enabledCoinTypes)
        val kitMarketCoins = marketKit.marketCoinsByCoinTypes(featuredCoinTypes + enabledCoinTypes)

        return appMarketCoins + kitMarketCoins
    }

    override fun marketCoins(filter: String, limit: Int): List<MarketCoin> {
        val appMarketCoins = customMarketCoins(filter)
        val kitMarketCoins = marketKit.marketCoins(filter, limit)

        return appMarketCoins + kitMarketCoins
    }

    override fun getPlatformCoin(coinType: CoinType): PlatformCoin? {
        return marketKit.platformCoin(coinType) ?: customPlatformCoin(coinType)
    }

    override fun getPlatformCoins(): List<PlatformCoin> {
        return marketKit.platformCoins() + customPlatformCoins()
    }

    override fun getPlatformCoins(coinTypes: List<CoinType>): List<PlatformCoin> {
        return marketKit.platformCoins(coinTypes)
    }

    override fun getPlatformCoinsByCoinTypeIds(coinTypeIds: List<String>): List<PlatformCoin> {
        val kitPlatformCoins = marketKit.platformCoinsByCoinTypeIds(coinTypeIds)
        val appPlatformCoins = customPlatformCoins(coinTypeIds)

        return kitPlatformCoins + appPlatformCoins
    }

    override fun save(customTokens: List<CustomToken>) {
        storage.save(customTokens)
    }

    private fun adjustedCustomTokens(customTokens: List<CustomToken>): List<CustomToken> {
        val existingPlatformCoins = marketKit.platformCoins(customTokens.map { it.coinType })
        return customTokens.filter { customToken -> !existingPlatformCoins.any { it.coinType == customToken.coinType } }
    }

    private fun customMarketCoins(filter: String): List<MarketCoin> {
        val customTokens = storage.customTokens(filter)
        return adjustedCustomTokens(customTokens).map { it.platformCoin.marketCoin }
    }

    private fun customMarketCoins(coinTypes: List<CoinType>): List<MarketCoin> {
        val customTokens = storage.customTokens(coinTypes.map { it.id })
        return adjustedCustomTokens(customTokens).map { it.platformCoin.marketCoin }
    }

    private fun customPlatformCoins(): List<PlatformCoin> {
        val customTokens = storage.customTokens()
        return adjustedCustomTokens(customTokens).map { it.platformCoin }
    }

    private fun customPlatformCoins(coinTypeIds: List<String>): List<PlatformCoin> {
        val customTokens = storage.customTokens(coinTypeIds)
        return adjustedCustomTokens(customTokens).map { it.platformCoin }
    }

    private fun customPlatformCoin(coinType: CoinType): PlatformCoin? {
        return storage.customToken(coinType)?.platformCoin
    }

}

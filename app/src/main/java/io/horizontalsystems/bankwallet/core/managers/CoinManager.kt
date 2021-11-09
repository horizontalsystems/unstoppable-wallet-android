package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ICustomTokenStorage
import io.horizontalsystems.bankwallet.entities.CustomToken
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.horizontalsystems.marketkit.models.PlatformType

class CoinManager(
    private val marketKit: MarketKit,
    private val storage: ICustomTokenStorage
) : ICoinManager {

    private val featuredCoinUids = listOf(
        "bitcoin",
        "ethereum",
        "bitcoin-cash",
        "zcash",
        "binancecoin",
        "dash",
        "litecoin",
        "uniswap",
        "sushi",
        "pancakeswap-token",
        "havven",
        "1inch",
        "curve-dao-token",
        "0x",
        "bancor",
        "balancer",
        "republic-protocol",
        "tether",
        "usd-coin",
        "binance-usd",
        "dai",
        "aave",
        "maker",
        "compound-governance-token",
        "yearn-finance",
        "badger-dao",
        "chainlink"
    )

    override fun featuredFullCoins(enabledPlatformCoins: List<PlatformCoin>): List<FullCoin> {
        val appFullCoins = customFullCoins(enabledPlatformCoins.map { it.coinType })
        val kitFullCoins = marketKit.fullCoins(featuredCoinUids + enabledPlatformCoins.map { it.coin.uid })

        return appFullCoins + kitFullCoins
    }

    override fun fullCoins(filter: String, limit: Int): List<FullCoin> {
        val appFullCoins = customFullCoins(filter)
        val kitFullCoins = marketKit.fullCoins(filter, limit)

        return appFullCoins + kitFullCoins
    }

    override fun getFullCoin(coinUid: String): FullCoin? {
        return marketKit.fullCoins(listOf(coinUid)).firstOrNull()
    }

    override fun getPlatformCoin(coinType: CoinType): PlatformCoin? {
        return marketKit.platformCoin(coinType) ?: customPlatformCoin(coinType)
    }

    override fun getPlatformCoins(platformType: PlatformType, filter: String, limit: Int): List<PlatformCoin> {
        return marketKit.platformCoins(platformType, filter, limit) + customPlatformCoins(platformType, filter)
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

    private fun customFullCoinsFromCustomTokens(customTokens: List<CustomToken>): List<FullCoin> {
        val platformCoins = customTokens.map { it.platformCoin }
        val groupedPlatformCoins = platformCoins.groupBy { it.coin }

        return groupedPlatformCoins.map { (coin, platformCoins) ->
            FullCoin(coin, platformCoins.map { it.platform })
        }
    }

    private fun adjustedCustomTokens(customTokens: List<CustomToken>): List<CustomToken> {
        val existingPlatformCoins = marketKit.platformCoins(customTokens.map { it.coinType })
        return customTokens.filter { customToken -> !existingPlatformCoins.any { it.coinType == customToken.coinType } }
    }

    private fun customFullCoins(filter: String): List<FullCoin> {
        val customTokens = storage.customTokens(filter)
        val adjustedCustomTokens = adjustedCustomTokens(customTokens)
        return customFullCoinsFromCustomTokens(adjustedCustomTokens)
    }

    private fun customFullCoins(coinTypes: List<CoinType>): List<FullCoin> {
        val customTokens = storage.customTokens(coinTypes.map { it.id })
        val adjustedCustomTokens = adjustedCustomTokens(customTokens)
        return customFullCoinsFromCustomTokens(adjustedCustomTokens)
    }

    private fun customPlatformCoins(platformType: PlatformType, filter: String): List<PlatformCoin> {
        val customTokens = storage.customTokens(platformType, filter)
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

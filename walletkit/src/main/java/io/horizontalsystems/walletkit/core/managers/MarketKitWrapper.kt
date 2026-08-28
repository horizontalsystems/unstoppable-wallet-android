package io.horizontalsystems.walletkit.core.managers

import android.content.Context
import io.horizontalsystems.walletkit.core.InvalidAuthTokenException
import io.horizontalsystems.walletkit.core.NoAuthTokenException
import io.horizontalsystems.walletkit.core.customCoinPrefix
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.SyncInfo
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.HsPeriodType
import io.horizontalsystems.marketkit.models.HsPointTimePeriod
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.horizontalsystems.marketkit.models.MarketInfo
import io.horizontalsystems.marketkit.models.NftTopCollection
import io.horizontalsystems.marketkit.models.Stock
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.Vault
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import retrofit2.Response
import java.math.BigDecimal

class MarketKitWrapper(
    context: Context,
    hsApiBaseUrl: String,
    hsApiKey: String,
    newsApiKey: String,
) {
    val userSubscriptionManager = UserSubscriptionManager

    private val marketKit: MarketKit by lazy {
        MarketKit.getInstance(
            context = context,
            hsApiBaseUrl = hsApiBaseUrl,
            hsApiKey = hsApiKey,
            newsApiKey = newsApiKey
        )
    }

    private suspend fun <T> requestWithAuthToken(f: suspend (String) -> T): T {
        val authToken = userSubscriptionManager.authToken ?: throw NoAuthTokenException()
        try {
            return f.invoke(authToken)
        } catch (error: HttpException) {
            if (error.code() == 401 || error.code() == 403) {
                userSubscriptionManager.authToken = null
                throw InvalidAuthTokenException()
            }
            throw error
        }
    }

    private fun <T> neverFlow(): Flow<T> = flow { awaitCancellation() }

    // Coins

    val fullCoinsUpdatedObservable: SharedFlow<Unit>
        get() = marketKit.fullCoinsUpdatedObservable

    fun topFullCoins(limit: Int = 20) = marketKit.topFullCoins(limit)

    fun fullCoins(filter: String, limit: Int = 20) = marketKit.fullCoins(filter, limit)

    fun fullCoins(coinUids: List<String>) = marketKit.fullCoins(coinUids)

    fun fullCoinsByCoinCode(coinCodes: List<String>) = marketKit.fullCoinsByCoinCodes(coinCodes)

    fun allCoins() = marketKit.allCoins()

    fun token(query: TokenQuery) = marketKit.token(query)

    fun tokens(queries: List<TokenQuery>) = marketKit.tokens(queries)

    fun tokens(reference: String) = marketKit.tokens(reference)

    fun tokens(blockchainType: BlockchainType, filter: String, limit: Int = 20) = marketKit.tokens(blockchainType, filter, limit)

    fun allBlockchains() = marketKit.allBlockchains()

    fun blockchains(uids: List<String>) = marketKit.blockchains(uids)

    fun blockchain(uid: String) = marketKit.blockchain(uid)

    suspend fun marketInfosSingle(top: Int, currencyCode: String, defi: Boolean) = marketKit.marketInfosSingle(top, currencyCode, defi)

    suspend fun categoriesSingle() = marketKit.categoriesSingle()

    suspend fun advancedMarketInfosSingle(top: Int = 250, currencyCode: String) = marketKit.advancedMarketInfosSingle(top, currencyCode)

    suspend fun marketInfosSingle(coinUids: List<String>, currencyCode: String): List<MarketInfo> =
        marketKit.marketInfosSingle(coinUids.removeCustomCoins(), currencyCode)

    suspend fun marketInfosSingle(categoryUid: String, currencyCode: String) = marketKit.marketInfosSingle(categoryUid, currencyCode)

    suspend fun marketInfoOverviewSingle(
        coinUid: String,
        currencyCode: String,
        language: String,
        roiUids: List<String>,
        roiPeriods: List<HsTimePeriod>
    ) = marketKit.marketInfoOverviewSingle(coinUid, currencyCode, language, roiUids, roiPeriods)

    suspend fun analyticsSingle(coinUid: String, currencyCode: String) =
        requestWithAuthToken { marketKit.analyticsSingle(it, coinUid, currencyCode) }

    suspend fun analyticsPreviewSingle(coinUid: String, addresses: List<String>) = marketKit.analyticsPreviewSingle(coinUid, addresses)

    suspend fun marketInfoTvlSingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod) =
        marketKit.marketInfoTvlSingle(coinUid, currencyCode, timePeriod)

    suspend fun marketInfoGlobalTvlSingle(chain: String, currencyCode: String, timePeriod: HsTimePeriod) =
        marketKit.marketInfoGlobalTvlSingle(chain, currencyCode, timePeriod)

    suspend fun defiMarketInfosSingle(currencyCode: String) = marketKit.defiMarketInfosSingle(currencyCode)

    // Categories

    suspend fun coinCategoriesSingle(currencyCode: String) = marketKit.coinCategoriesSingle(currencyCode)

    suspend fun coinCategoryMarketPointsSingle(categoryUid: String, interval: HsTimePeriod, currencyCode: String) =
        marketKit.coinCategoryMarketPointsSingle(categoryUid, interval, currencyCode)

    fun sync() = marketKit.sync()

    // Coin Prices

    private val String.isCustomCoin: Boolean
        get() = startsWith(TokenQuery.customCoinPrefix)

    private fun List<String>.removeCustomCoins(): List<String> = filterNot { it.isCustomCoin }

    fun refreshCoinPrices(currencyCode: String) = marketKit.refreshCoinPrices(currencyCode)

    fun coinPrice(coinUid: String, currencyCode: String): CoinPrice? =
        if (coinUid.isCustomCoin) null else marketKit.coinPrice(coinUid, currencyCode)

    fun coinPriceMap(coinUids: List<String>, currencyCode: String): Map<String, CoinPrice> {
        val coinUidsNoCustom = coinUids.removeCustomCoins()
        return when {
            coinUidsNoCustom.isEmpty() -> mapOf()
            else -> marketKit.coinPriceMap(coinUidsNoCustom, currencyCode)
        }
    }

    fun coinPriceObservable(tag: String, coinUid: String, currencyCode: String): Flow<CoinPrice> =
        if (coinUid.isCustomCoin) neverFlow() else marketKit.coinPriceObservable(tag, coinUid, currencyCode)

    fun coinPriceMapObservable(tag: String, coinUids: List<String>, currencyCode: String): Flow<Map<String, CoinPrice>> {
        val coinUidsNoCustom = coinUids.removeCustomCoins()
        return when {
            coinUidsNoCustom.isEmpty() -> neverFlow()
            else -> marketKit.coinPriceMapObservable(tag, coinUidsNoCustom, currencyCode)
        }
    }

    // Coin Historical Price

    suspend fun coinHistoricalPriceSingle(coinUid: String, currencyCode: String, timestamp: Long): BigDecimal? =
        if (coinUid.isCustomCoin) null else marketKit.coinHistoricalPriceSingle(coinUid, currencyCode, timestamp)

    fun coinHistoricalPrice(coinUid: String, currencyCode: String, timestamp: Long) =
        if (coinUid.isCustomCoin) null else marketKit.coinHistoricalPrice(coinUid, currencyCode, timestamp)

    // Posts

    suspend fun postsSingle() = marketKit.postsSingle()

    // Market Tickers

    suspend fun marketTickersSingle(coinUid: String, currencyCode: String) = marketKit.marketTickersSingle(coinUid, currencyCode)

    // Details

    suspend fun tokenHoldersSingle(coinUid: String, blockchainUid: String) =
        requestWithAuthToken { marketKit.tokenHoldersSingle(it, coinUid, blockchainUid) }

    suspend fun treasuriesSingle(coinUid: String, currencyCode: String) = marketKit.treasuriesSingle(coinUid, currencyCode)

    suspend fun investmentsSingle(coinUid: String) = marketKit.investmentsSingle(coinUid)

    suspend fun coinReportsSingle(coinUid: String) = marketKit.coinReportsSingle(coinUid)

    // Pro Details

    suspend fun cexVolumesSingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod) =
        marketKit.cexVolumesSingle(coinUid, currencyCode, timePeriod)

    suspend fun dexLiquiditySingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod) =
        requestWithAuthToken { marketKit.dexLiquiditySingle(it, coinUid, currencyCode, timePeriod) }

    suspend fun dexVolumesSingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod) =
        requestWithAuthToken { marketKit.dexVolumesSingle(it, coinUid, currencyCode, timePeriod) }

    suspend fun transactionDataSingle(coinUid: String, timePeriod: HsTimePeriod, platform: String?) =
        requestWithAuthToken { marketKit.transactionDataSingle(it, coinUid, timePeriod, platform) }

    suspend fun activeAddressesSingle(coinUid: String, timePeriod: HsTimePeriod) =
        requestWithAuthToken { marketKit.activeAddressesSingle(it, coinUid, timePeriod) }

    suspend fun cexVolumeRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.cexVolumeRanksSingle(it, currencyCode) }

    suspend fun dexVolumeRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.dexVolumeRanksSingle(it, currencyCode) }

    suspend fun dexLiquidityRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.dexLiquidityRanksSingle(it, currencyCode) }

    suspend fun activeAddressRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.activeAddressRanksSingle(it, currencyCode) }

    suspend fun transactionCountsRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.transactionCountsRanksSingle(it, currencyCode) }

    suspend fun revenueRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.revenueRanksSingle(it, currencyCode) }

    suspend fun feeRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.feeRanksSingle(it, currencyCode) }

    suspend fun holdersRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.holderRanksSingle(it, currencyCode) }

    // Overview

    suspend fun marketOverviewSingle(currencyCode: String) = marketKit.marketOverviewSingle(currencyCode)

    suspend fun marketGlobalSingle(currencyCode: String) = marketKit.marketGlobalSingle(currencyCode)

    suspend fun topPairsSingle(currencyCode: String, page: Int, limit: Int) = marketKit.topPairsSingle(currencyCode, page, limit)

    suspend fun topMoversSingle(currencyCode: String) = marketKit.topMoversSingle(currencyCode)

    suspend fun topCoinsMarketInfosSingle(top: Int, currencyCode: String) = marketKit.topCoinsMarketInfosSingle(top, currencyCode)

    // Chart Info

    suspend fun chartStartTimeSingle(coinUid: String) = marketKit.chartStartTimeSingle(coinUid)

    suspend fun chartPointsSingle(coinUid: String, currencyCode: String, periodType: HsPeriodType) =
        marketKit.chartPointsSingle(coinUid, currencyCode, periodType)

    suspend fun chartPointsSingle(coinUid: String, currencyCode: String, period: HsPointTimePeriod, pointCount: Int) =
        marketKit.chartPointsSingle(coinUid, currencyCode, period, pointCount)

    // Global Market Info

    suspend fun globalMarketPointsSingle(currencyCode: String, timePeriod: HsTimePeriod) =
        marketKit.globalMarketPointsSingle(currencyCode, timePeriod)

    suspend fun topPlatformsSingle(currencyCode: String) =
        marketKit.topPlatformsSingle(currencyCode)

    suspend fun topPlatformMarketCapStartTimeSingle(platform: String) =
        marketKit.topPlatformMarketCapStartTimeSingle(platform)

    suspend fun topPlatformMarketCapPointsSingle(
        chain: String,
        currencyCode: String,
        periodType: HsPeriodType
    ) = marketKit.topPlatformMarketCapPointsSingle(chain, currencyCode, periodType)

    suspend fun topPlatformCoinListSingle(chain: String, currencyCode: String) =
        marketKit.topPlatformMarketInfosSingle(chain, currencyCode)

    suspend fun getCoinSignalsSingle(coinUids: List<String>) = marketKit.coinsSignalsSingle(coinUids)

    // NFT

    suspend fun nftCollections(): List<NftTopCollection> =
        marketKit.nftTopCollections()

    suspend fun subscriptionsSingle(addresses: List<String>) =
        marketKit.subscriptionsSingle(addresses)

    suspend fun authGetSignMessage(address: String) =
        marketKit.authGetSignMessage(address)

    suspend fun authenticate(signature: String, address: String) =
        marketKit.authenticate(signature, address)

    // Misc

    fun syncInfo(): SyncInfo {
        return marketKit.syncInfo()
    }

    suspend fun requestPersonalSupport(username: String): Response<Void> =
        requestWithAuthToken { marketKit.requestPersonalSupport(it, username) }

    suspend fun requestVipSupport(subscriptionId: String): Map<String, String> =
        requestWithAuthToken { marketKit.requestVipSupport(it, subscriptionId) }

    suspend fun getStocks(currencyCode: String): List<Stock> = marketKit.getStocks(currencyCode)

    // Stats

    suspend fun sendStats(stats: String, appVersion: String, appId: String?): Unit {
        return marketKit.sendStats(stats, appVersion, appId)
    }

    // Etf

    suspend fun etfs(category: String, currencyCode: String) = marketKit.etfSingle(category, currencyCode)

    suspend fun etfPoints(category: String, currencyCode: String, period: String) = marketKit.etfPointSingle(category, currencyCode, period)

    // Vaults

    suspend fun vaults(currencyCode: String): List<Vault> {
        return requestWithAuthToken { marketKit.vaultsSingle(currencyCode) }
    }

    suspend fun vault(address: String, currencyCode: String, periodType: HsTimePeriod): Vault {
        return requestWithAuthToken { marketKit.vaultSingle(address, currencyCode, periodType) }
    }

}

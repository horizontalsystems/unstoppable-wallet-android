package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import io.horizontalsystems.bankwallet.core.InvalidAuthTokenException
import io.horizontalsystems.bankwallet.core.NoAuthTokenException
import io.horizontalsystems.bankwallet.core.customCoinPrefix
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.SyncInfo
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.HsPeriodType
import io.horizontalsystems.marketkit.models.HsPointTimePeriod
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.horizontalsystems.marketkit.models.MarketInfo
import io.horizontalsystems.marketkit.models.NftTopCollection
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.HttpException
import retrofit2.Response
import java.math.BigDecimal

class MarketKitWrapper(
    context: Context,
    hsApiBaseUrl: String,
    hsApiKey: String,
) {
    val userSubscriptionManager = UserSubscriptionManager

    private val marketKit: MarketKit by lazy {
        MarketKit.getInstance(
            context = context,
            hsApiBaseUrl = hsApiBaseUrl,
            hsApiKey = hsApiKey,
        )
    }

    private fun <T> requestWithAuthToken(f: (String) -> Single<T>) =
        userSubscriptionManager.authToken?.let { authToken ->
            f.invoke(authToken).onErrorResumeNext { error ->
                if (error is HttpException && (error.code() == 401 || error.code() == 403)) {
                    userSubscriptionManager.authToken = null

                    Single.error(InvalidAuthTokenException())
                } else {
                    Single.error(error)
                }
            }
        } ?: run {
            Single.error(NoAuthTokenException())
        }

    // Coins

    val fullCoinsUpdatedObservable: Observable<Unit>
        get() = marketKit.fullCoinsUpdatedObservable

    fun topFullCoins(limit: Int = 20) = marketKit.topFullCoins(limit)

    fun fullCoins(filter: String, limit: Int = 20) = marketKit.fullCoins(filter, limit)

    fun fullCoins(coinUids: List<String>) = marketKit.fullCoins(coinUids)

    fun allCoins() = marketKit.allCoins()

    fun token(query: TokenQuery) = marketKit.token(query)

    fun tokens(queries: List<TokenQuery>) = marketKit.tokens(queries)

    fun tokens(reference: String) = marketKit.tokens(reference)

    fun tokens(blockchainType: BlockchainType, filter: String, limit: Int = 20) = marketKit.tokens(blockchainType, filter, limit)

    fun allBlockchains() = marketKit.allBlockchains()

    fun blockchains(uids: List<String>) = marketKit.blockchains(uids)

    fun blockchain(uid: String) = marketKit.blockchain(uid)

    fun marketInfosSingle(top: Int, currencyCode: String, defi: Boolean) = marketKit.marketInfosSingle(top, currencyCode, defi)

    fun categoriesSingle() = marketKit.categoriesSingle()

    fun advancedMarketInfosSingle(top: Int = 250, currencyCode: String) = marketKit.advancedMarketInfosSingle(top, currencyCode)

    fun marketInfosSingle(coinUids: List<String>, currencyCode: String): Single<List<MarketInfo>> =
        marketKit.marketInfosSingle(coinUids.removeCustomCoins(), currencyCode)

    fun marketInfosSingle(categoryUid: String, currencyCode: String) = marketKit.marketInfosSingle(categoryUid, currencyCode)

    fun marketInfoOverviewSingle(
        coinUid: String,
        currencyCode: String,
        language: String,
        roiUids: List<String>,
        roiPeriods: List<HsTimePeriod>
    ) = marketKit.marketInfoOverviewSingle(coinUid, currencyCode, language, roiUids, roiPeriods)

    fun analyticsSingle(coinUid: String, currencyCode: String) =
        requestWithAuthToken { marketKit.analyticsSingle(it, coinUid, currencyCode) }

    fun analyticsPreviewSingle(coinUid: String, addresses: List<String>) = marketKit.analyticsPreviewSingle(coinUid, addresses)

    fun marketInfoTvlSingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod) =
        marketKit.marketInfoTvlSingle(coinUid, currencyCode, timePeriod)

    fun marketInfoGlobalTvlSingle(chain: String, currencyCode: String, timePeriod: HsTimePeriod) =
        marketKit.marketInfoGlobalTvlSingle(chain, currencyCode, timePeriod)

    fun defiMarketInfosSingle(currencyCode: String) = marketKit.defiMarketInfosSingle(currencyCode)

    // Categories

    fun coinCategoriesSingle(currencyCode: String) = marketKit.coinCategoriesSingle(currencyCode)

    fun coinCategoryMarketPointsSingle(categoryUid: String, interval: HsTimePeriod, currencyCode: String) =
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

    fun coinPriceObservable(tag: String, coinUid: String, currencyCode: String): Observable<CoinPrice> =
        if (coinUid.isCustomCoin) Observable.never() else marketKit.coinPriceObservable(tag, coinUid, currencyCode)

    fun coinPriceMapObservable(tag: String, coinUids: List<String>, currencyCode: String): Observable<Map<String, CoinPrice>> {
        val coinUidsNoCustom = coinUids.removeCustomCoins()
        return when {
            coinUidsNoCustom.isEmpty() -> Observable.never()
            else -> marketKit.coinPriceMapObservable(tag, coinUidsNoCustom, currencyCode)
        }
    }

    // Coin Historical Price

    fun coinHistoricalPriceSingle(coinUid: String, currencyCode: String, timestamp: Long): Single<BigDecimal> =
        if (coinUid.isCustomCoin) Single.never() else marketKit.coinHistoricalPriceSingle(coinUid, currencyCode, timestamp)

    fun coinHistoricalPrice(coinUid: String, currencyCode: String, timestamp: Long) =
        if (coinUid.isCustomCoin) null else marketKit.coinHistoricalPrice(coinUid, currencyCode, timestamp)

    // Posts

    fun postsSingle() = marketKit.postsSingle()

    // Market Tickers

    fun marketTickersSingle(coinUid: String, currencyCode: String) = marketKit.marketTickersSingle(coinUid, currencyCode)

    // Details

    fun tokenHoldersSingle(coinUid: String, blockchainUid: String) =
        requestWithAuthToken { marketKit.tokenHoldersSingle(it, coinUid, blockchainUid) }

    fun treasuriesSingle(coinUid: String, currencyCode: String) = marketKit.treasuriesSingle(coinUid, currencyCode)

    fun investmentsSingle(coinUid: String) = marketKit.investmentsSingle(coinUid)

    fun coinReportsSingle(coinUid: String) = marketKit.coinReportsSingle(coinUid)

    // Pro Details

    fun cexVolumesSingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod) =
        marketKit.cexVolumesSingle(coinUid, currencyCode, timePeriod)

    fun dexLiquiditySingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod) =
        requestWithAuthToken { marketKit.dexLiquiditySingle(it, coinUid, currencyCode, timePeriod) }

    fun dexVolumesSingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod) =
        requestWithAuthToken { marketKit.dexVolumesSingle(it, coinUid, currencyCode, timePeriod) }

    fun transactionDataSingle(coinUid: String, timePeriod: HsTimePeriod, platform: String?) =
        requestWithAuthToken { marketKit.transactionDataSingle(it, coinUid, timePeriod, platform) }

    fun activeAddressesSingle(coinUid: String, timePeriod: HsTimePeriod) =
        requestWithAuthToken { marketKit.activeAddressesSingle(it, coinUid, timePeriod) }

    fun cexVolumeRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.cexVolumeRanksSingle(it, currencyCode) }

    fun dexVolumeRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.dexVolumeRanksSingle(it, currencyCode) }

    fun dexLiquidityRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.dexLiquidityRanksSingle(it, currencyCode) }

    fun activeAddressRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.activeAddressRanksSingle(it, currencyCode) }

    fun transactionCountsRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.transactionCountsRanksSingle(it, currencyCode) }

    fun revenueRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.revenueRanksSingle(it, currencyCode) }

    fun feeRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.feeRanksSingle(it, currencyCode) }

    fun holdersRanksSingle(currencyCode: String) =
        requestWithAuthToken { marketKit.holderRanksSingle(it, currencyCode) }

    // Overview

    fun marketOverviewSingle(currencyCode: String) = marketKit.marketOverviewSingle(currencyCode)

    fun marketGlobalSingle(currencyCode: String) = marketKit.marketGlobalSingle(currencyCode)

    fun topPairsSingle(currencyCode: String, page: Int, limit: Int) = marketKit.topPairsSingle(currencyCode, page, limit)

    fun topMoversSingle(currencyCode: String) = marketKit.topMoversSingle(currencyCode)

    fun topCoinsMarketInfosSingle(top: Int, currencyCode: String) = marketKit.topCoinsMarketInfosSingle(top, currencyCode)

    // Chart Info

    fun chartStartTimeSingle(coinUid: String) = marketKit.chartStartTimeSingle(coinUid)

    fun chartPointsSingle(coinUid: String, currencyCode: String, periodType: HsPeriodType) =
        marketKit.chartPointsSingle(coinUid, currencyCode, periodType)

    fun chartPointsSingle(coinUid: String, currencyCode: String, period: HsPointTimePeriod, pointCount: Int) =
        marketKit.chartPointsSingle(coinUid, currencyCode, period, pointCount)

    // Global Market Info

    fun globalMarketPointsSingle(currencyCode: String, timePeriod: HsTimePeriod) =
        marketKit.globalMarketPointsSingle(currencyCode, timePeriod)

    fun topPlatformsSingle(currencyCode: String) =
        marketKit.topPlatformsSingle(currencyCode)

    fun topPlatformMarketCapStartTimeSingle(platform: String) =
        marketKit.topPlatformMarketCapStartTimeSingle(platform)

    fun topPlatformMarketCapPointsSingle(
        chain: String,
        currencyCode: String,
        periodType: HsPeriodType
    ) = marketKit.topPlatformMarketCapPointsSingle(chain, currencyCode, periodType)

    fun topPlatformCoinListSingle(chain: String, currencyCode: String) =
        marketKit.topPlatformMarketInfosSingle(chain, currencyCode)

    fun getCoinSignalsSingle(coinUids: List<String>) = marketKit.coinsSignalsSingle(coinUids)

    // NFT

    suspend fun nftCollections(): List<NftTopCollection> =
        marketKit.nftTopCollections()

    fun subscriptionsSingle(addresses: List<String>) =
        marketKit.subscriptionsSingle(addresses)

    fun authGetSignMessage(address: String) =
        marketKit.authGetSignMessage(address)

    fun authenticate(signature: String, address: String) =
        marketKit.authenticate(signature, address)

    // Misc

    fun syncInfo(): SyncInfo {
        return marketKit.syncInfo()
    }

    fun requestPersonalSupport(username: String): Single<Response<Void>> =
        requestWithAuthToken { marketKit.requestPersonalSupport(it, username) }

    fun requestVipSupport(subscriptionId: String): Single<Map<String, String>> =
        requestWithAuthToken { marketKit.requestVipSupport(it, subscriptionId) }

    // Stats

    fun sendStats(stats: String, appVersion: String, appId: String?): Single<Unit> {
        return marketKit.sendStats(stats, appVersion, appId)
    }

    // Etf

    fun etfs(currencyCode: String) = marketKit.etfSingle(currencyCode)

    fun etfPoints(currencyCode: String) = marketKit.etfPointSingle(currencyCode)

}

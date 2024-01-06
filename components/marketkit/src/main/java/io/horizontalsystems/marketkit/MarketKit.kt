package io.horizontalsystems.marketkit

import android.content.Context
import android.os.storage.StorageManager
import io.horizontalsystems.marketkit.chart.HsChartRequestHelper
import io.horizontalsystems.marketkit.managers.*
import io.horizontalsystems.marketkit.models.*
import io.horizontalsystems.marketkit.providers.*
import io.horizontalsystems.marketkit.storage.*
import io.horizontalsystems.marketkit.syncers.CoinSyncer
import io.horizontalsystems.marketkit.syncers.ExchangeSyncer
import io.horizontalsystems.marketkit.syncers.HsDataSyncer
import io.horizontalsystems.marketkit.syncers.VerifiedExchangeSyncer
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.Response
import java.math.BigDecimal
import java.util.*

class MarketKit(
    private val nftManager: NftManager,
    private val marketOverviewManager: MarketOverviewManager,
    private val coinManager: CoinManager,
    private val coinSyncer: CoinSyncer,
    private val coinPriceManager: CoinPriceManager,
    private val coinHistoricalPriceManager: CoinHistoricalPriceManager,
    private val coinPriceSyncManager: CoinPriceSyncManager,
    private val postManager: PostManager,
    private val exchangeSyncer: ExchangeSyncer,
    private val globalMarketInfoManager: GlobalMarketInfoManager,
    private val hsProvider: HsProvider,
    private val hsDataSyncer: HsDataSyncer,
    private val dumpManager: DumpManager,
    private val coinGeckoProvider: CoinGeckoProvider,
    private val exchangeManager: ExchangeManager,
    private val defiYieldProvider: DefiYieldProvider,
) {
    // Coins

    val fullCoinsUpdatedObservable: Observable<Unit>
        get() = coinSyncer.fullCoinsUpdatedObservable

    fun fullCoins(filter: String, limit: Int = 20): List<FullCoin> {
        return coinManager.fullCoins(filter, limit)
    }

    fun fullCoins(coinUids: List<String>): List<FullCoin> {
        return coinManager.fullCoins(coinUids)
    }

    fun allCoins(): List<Coin> = coinManager.allCoins()

    fun token(query: TokenQuery): Token? =
        coinManager.token(query)

    fun tokens(queries: List<TokenQuery>): List<Token> =
        coinManager.tokens(queries)

    fun tokens(reference: String): List<Token> =
        coinManager.tokens(reference)

    fun tokens(blockchainType: BlockchainType, filter: String, limit: Int = 20): List<Token> =
        coinManager.tokens(blockchainType, filter, limit)

    fun blockchains(uids: List<String>): List<Blockchain> =
        coinManager.blockchains(uids)

    fun allBlockchains(): List<Blockchain> =
        coinManager.allBlockchains()

    fun blockchain(uid: String): Blockchain? =
        coinManager.blockchain(uid)

    fun marketInfosSingle(
        top: Int,
        currencyCode: String,
        defi: Boolean,
        apiTag: String
    ): Single<List<MarketInfo>> {
        return hsProvider.marketInfosSingle(top, currencyCode, defi, apiTag).map {
            coinManager.getMarketInfos(it)
        }
    }

    fun advancedMarketInfosSingle(
        top: Int = 250,
        currencyCode: String,
        apiTag: String
    ): Single<List<MarketInfo>> {
        return hsProvider.advancedMarketInfosSingle(top, currencyCode, apiTag).map {
            coinManager.getMarketInfos(it)
        }
    }

    fun marketInfosSingle(
        coinUids: List<String>,
        currencyCode: String,
        apiTag: String
    ): Single<List<MarketInfo>> {
        return hsProvider.marketInfosSingle(coinUids, currencyCode, apiTag).map {
            coinManager.getMarketInfos(it)
        }
    }

    fun marketInfosSingle(
        categoryUid: String,
        currencyCode: String,
        apiTag: String
    ): Single<List<MarketInfo>> {
        return hsProvider.marketInfosSingle(categoryUid, currencyCode, apiTag).map {
            coinManager.getMarketInfos(it)
        }
    }

    fun marketInfoOverviewSingle(
        coinUid: String,
        currencyCode: String,
        language: String,
        apiTag: String,
    ): Single<MarketInfoOverview> {
        return hsProvider.getMarketInfoOverview(
            coinUid = coinUid,
            currencyCode = currencyCode,
            language = language,
            apiTag = apiTag,
        ).map { rawOverview ->
            val fullCoin = coinManager.fullCoin(coinUid) ?: throw Exception("No Full Coin")

            rawOverview.marketInfoOverview(fullCoin)
        }
    }

    fun marketInfoTvlSingle(
        coinUid: String,
        currencyCode: String,
        timePeriod: HsTimePeriod
    ): Single<List<ChartPoint>> {
        return hsProvider.marketInfoTvlSingle(coinUid, currencyCode, timePeriod)
    }

    fun marketInfoGlobalTvlSingle(
        chain: String,
        currencyCode: String,
        timePeriod: HsTimePeriod
    ): Single<List<ChartPoint>> {
        return hsProvider.marketInfoGlobalTvlSingle(chain, currencyCode, timePeriod)
    }

    fun defiMarketInfosSingle(currencyCode: String, apiTag: String): Single<List<DefiMarketInfo>> {
        return hsProvider.defiMarketInfosSingle(currencyCode, apiTag).map {
            coinManager.getDefiMarketInfos(it)
        }
    }

    // Categories

    fun coinCategoriesSingle(currencyCode: String): Single<List<CoinCategory>> =
        hsProvider.getCoinCategories(currencyCode)

    fun coinCategoryMarketPointsSingle(
        categoryUid: String,
        interval: HsTimePeriod,
        currencyCode: String
    ) =
        hsProvider.coinCategoryMarketPointsSingle(categoryUid, interval, currencyCode)

    fun sync() {
        hsDataSyncer.sync()
        exchangeSyncer.sync()
    }

    // Coin Prices

    fun refreshCoinPrices(currencyCode: String) {
        coinPriceSyncManager.refresh(currencyCode)
    }

    fun coinPrice(coinUid: String, currencyCode: String): CoinPrice? {
        return coinPriceManager.coinPrice(coinUid, currencyCode)
    }

    fun coinPriceMap(coinUids: List<String>, currencyCode: String): Map<String, CoinPrice> {
        return coinPriceManager.coinPriceMap(coinUids, currencyCode)
    }

    fun coinPriceObservable(
        tag: String,
        coinUid: String,
        currencyCode: String
    ): Observable<CoinPrice> {
        return coinPriceSyncManager.coinPriceObservable(tag, coinUid, currencyCode)
    }

    fun coinPriceMapObservable(
        tag: String,
        coinUids: List<String>,
        currencyCode: String
    ): Observable<Map<String, CoinPrice>> {
        return coinPriceSyncManager.coinPriceMapObservable(tag, coinUids, currencyCode)
    }

    // Coin Historical Price

    fun coinHistoricalPriceSingle(
        coinUid: String,
        currencyCode: String,
        timestamp: Long
    ): Single<BigDecimal> {
        return coinHistoricalPriceManager.coinHistoricalPriceSingle(
            coinUid,
            currencyCode,
            timestamp
        )
    }

    fun coinHistoricalPrice(coinUid: String, currencyCode: String, timestamp: Long): BigDecimal? {
        return coinHistoricalPriceManager.coinHistoricalPrice(coinUid, currencyCode, timestamp)
    }

    // Posts

    fun postsSingle(): Single<List<Post>> {
        return postManager.postsSingle()
    }

    // Market Tickers

    fun marketTickersSingle(coinUid: String): Single<List<MarketTicker>> {
        val coinGeckoId = coinManager.coin(coinUid)?.coinGeckoId ?: return Single.just(emptyList())

        return coinGeckoProvider.marketTickersSingle(coinGeckoId)
            .map { response ->
                val coinUids =
                    (response.tickers.map { it.coinId } + response.tickers.mapNotNull { it.targetCoinId }).distinct()
                val coins = coinManager.coins(coinUids)
                val imageUrls = exchangeManager.imageUrlsMap(response.exchangeIds)
                val verifiedExchangeUids = exchangeManager.verifiedExchangeUids()
                response.marketTickers(verifiedExchangeUids, imageUrls, coins)
            }
    }

    // Details

    fun tokenHoldersSingle(
        authToken: String,
        coinUid: String,
        blockchainUid: String
    ): Single<TokenHolders> {
        return hsProvider.tokenHoldersSingle(authToken, coinUid, blockchainUid)
    }

    fun treasuriesSingle(coinUid: String, currencyCode: String): Single<List<CoinTreasury>> {
        return hsProvider.coinTreasuriesSingle(coinUid, currencyCode)
    }

    fun investmentsSingle(coinUid: String): Single<List<CoinInvestment>> {
        return hsProvider.investmentsSingle(coinUid)
    }

    fun coinReportsSingle(coinUid: String): Single<List<CoinReport>> {
        return hsProvider.coinReportsSingle(coinUid)
    }

    fun auditReportsSingle(addresses: List<String>): Single<List<Auditor>> {
        return defiYieldProvider.auditReportsSingle(addresses)
    }

    // Pro Data

    fun cexVolumesSingle(
        coinUid: String,
        currencyCode: String,
        timePeriod: HsTimePeriod
    ): Single<List<ChartPoint>> {
        val periodType = HsPeriodType.ByPeriod(timePeriod)
        val currentTime = Date().time / 1000
        val fromTimestamp = HsChartRequestHelper.fromTimestamp(currentTime, periodType)
        val interval = HsPointTimePeriod.Day1
        return hsProvider.coinPriceChartSingle(coinUid, currencyCode, interval, fromTimestamp)
            .map { response ->
                response.mapNotNull { chartCoinPrice ->
                    chartCoinPrice.totalVolume?.let { volume ->
                        ChartPoint(volume, chartCoinPrice.timestamp, null)
                    }
                }
            }
    }

    fun dexLiquiditySingle(
        authToken: String,
        coinUid: String,
        currencyCode: String,
        timePeriod: HsTimePeriod
    ): Single<List<Analytics.VolumePoint>> {
        return hsProvider.dexLiquiditySingle(authToken, coinUid, currencyCode, timePeriod)
    }

    fun dexVolumesSingle(
        authToken: String,
        coinUid: String,
        currencyCode: String,
        timePeriod: HsTimePeriod
    ): Single<List<Analytics.VolumePoint>> {
        return hsProvider.dexVolumesSingle(authToken, coinUid, currencyCode, timePeriod)
    }

    fun transactionDataSingle(
        authToken: String,
        coinUid: String,
        timePeriod: HsTimePeriod,
        platform: String?
    ): Single<List<Analytics.CountVolumePoint>> {
        return hsProvider.transactionDataSingle(authToken, coinUid, timePeriod, platform)
    }

    fun activeAddressesSingle(
        authToken: String,
        coinUid: String,
        timePeriod: HsTimePeriod
    ): Single<List<Analytics.CountPoint>> {
        return hsProvider.activeAddressesSingle(authToken, coinUid, timePeriod)
    }

    fun analyticsPreviewSingle(
        coinUid: String,
        addresses: List<String>,
        apiTag: String
    ): Single<AnalyticsPreview> {
        return hsProvider.analyticsPreviewSingle(coinUid, addresses, apiTag)
    }

    fun analyticsSingle(
        authToken: String,
        coinUid: String,
        currencyCode: String,
        apiTag: String
    ): Single<Analytics> {
        return hsProvider.analyticsSingle(authToken, coinUid, currencyCode, apiTag)
    }

    fun cexVolumeRanksSingle(
        authToken: String,
        currencyCode: String
    ): Single<List<RankMultiValue>> {
        return hsProvider.rankMultiValueSingle(authToken, "cex_volume", currencyCode)
    }

    fun dexVolumeRanksSingle(
        authToken: String,
        currencyCode: String
    ): Single<List<RankMultiValue>> {
        return hsProvider.rankMultiValueSingle(authToken, "dex_volume", currencyCode)
    }

    fun dexLiquidityRanksSingle(authToken: String, currencyCode: String): Single<List<RankValue>> {
        return hsProvider.rankValueSingle(authToken, "dex_liquidity", currencyCode)
    }

    fun activeAddressRanksSingle(
        authToken: String,
        currencyCode: String
    ): Single<List<RankMultiValue>> {
        return hsProvider.rankMultiValueSingle(authToken, "address", currencyCode)
    }

    fun transactionCountsRanksSingle(
        authToken: String,
        currencyCode: String
    ): Single<List<RankMultiValue>> {
        return hsProvider.rankMultiValueSingle(authToken, "tx_count", currencyCode)
    }

    fun holderRanksSingle(authToken: String, currencyCode: String): Single<List<RankValue>> {
        return hsProvider.rankValueSingle(authToken, "holders", currencyCode)
    }

    fun revenueRanksSingle(authToken: String, currencyCode: String): Single<List<RankMultiValue>> {
        return hsProvider.rankMultiValueSingle(authToken, "revenue", currencyCode)
    }

    fun feeRanksSingle(authToken: String, currencyCode: String): Single<List<RankMultiValue>> {
        return hsProvider.rankMultiValueSingle(authToken, "fee", currencyCode)
    }

    fun subscriptionsSingle(addresses: List<String>): Single<List<SubscriptionResponse>> {
        return hsProvider.subscriptionsSingle(addresses)
    }

    // Overview
    fun marketOverviewSingle(currencyCode: String): Single<MarketOverview> =
        marketOverviewManager.marketOverviewSingle(currencyCode)


    fun topMoversSingle(currencyCode: String): Single<TopMovers> =
        hsProvider.topMoversRawSingle(currencyCode)
            .map { raw ->
                TopMovers(
                    gainers100 = coinManager.getMarketInfos(raw.gainers100),
                    gainers200 = coinManager.getMarketInfos(raw.gainers200),
                    gainers300 = coinManager.getMarketInfos(raw.gainers300),
                    losers100 = coinManager.getMarketInfos(raw.losers100),
                    losers200 = coinManager.getMarketInfos(raw.losers200),
                    losers300 = coinManager.getMarketInfos(raw.losers300)
                )
            }

    // Chart Info

    fun chartPointsSingle(
        coinUid: String,
        currencyCode: String,
        interval: HsPointTimePeriod,
        pointCount: Int
    ): Single<List<ChartPoint>> {
        val fromTimestamp = Date().time / 1000 - interval.interval * pointCount

        return hsProvider.coinPriceChartSingle(coinUid, currencyCode, interval, fromTimestamp)
            .map { response ->
                response.map { chartCoinPrice ->
                    chartCoinPrice.chartPoint
                }
            }
    }

    fun chartPointsSingle(
        coinUid: String,
        currencyCode: String,
        periodType: HsPeriodType
    ): Single<Pair<Long, List<ChartPoint>>> {
        val interval = HsChartRequestHelper.pointInterval(periodType)
        val visibleTimestamp: Long
        val fromTimestamp: Long?
        when (periodType) {
            is HsPeriodType.ByPeriod -> {
                val currentTime = Date().time / 1000
                visibleTimestamp = HsChartRequestHelper.fromTimestamp(currentTime, periodType)
                fromTimestamp = visibleTimestamp
            }

            is HsPeriodType.ByCustomPoints -> {
                val currentTime = Date().time / 1000
                visibleTimestamp = HsChartRequestHelper.fromTimestamp(currentTime, periodType)
                val customPointsInterval = interval.interval * periodType.pointsCount
                fromTimestamp = visibleTimestamp - customPointsInterval
            }

            is HsPeriodType.ByStartTime -> {
                visibleTimestamp = periodType.startTime
                fromTimestamp = null
            }
        }
        return hsProvider.coinPriceChartSingle(coinUid, currencyCode, interval, fromTimestamp)
            .map {
                Pair(visibleTimestamp, it.map { it.chartPoint })
            }
    }

    fun chartStartTimeSingle(coinUid: String): Single<Long> {
        return hsProvider.coinPriceChartStartTime(coinUid)
    }

    // Global Market Info

    fun globalMarketPointsSingle(
        currencyCode: String,
        timePeriod: HsTimePeriod
    ): Single<List<GlobalMarketPoint>> {
        return globalMarketInfoManager.globalMarketInfoSingle(currencyCode, timePeriod)
    }

    fun topPlatformsSingle(currencyCode: String, apiTag: String): Single<List<TopPlatform>> {
        return hsProvider.topPlatformsSingle(currencyCode, apiTag)
            .map { responseList -> responseList.map { it.topPlatform } }
    }

    fun topPlatformMarketCapPointsSingle(
        chain: String,
        timePeriod: HsTimePeriod,
        currencyCode: String
    ): Single<List<TopPlatformMarketCapPoint>> {
        return hsProvider.topPlatformMarketCapPointsSingle(chain, timePeriod, currencyCode)
    }

    fun topPlatformMarketInfosSingle(
        chain: String,
        currencyCode: String,
        apiTag: String
    ): Single<List<MarketInfo>> {
        return hsProvider.topPlatformCoinListSingle(chain, currencyCode, apiTag)
            .map { coinManager.getMarketInfos(it) }
    }

    // NFT

    suspend fun nftTopCollections(): List<NftTopCollection> = nftManager.topCollections()

    // Auth

    fun authGetSignMessage(address: String): Single<String> {
        return hsProvider.authGetSignMessage(address)
    }

    fun authenticate(signature: String, address: String): Single<String> {
        return hsProvider.authenticate(signature, address)
    }

    fun requestPersonalSupport(authToken: String, username: String): Single<Response<Void>> {
        return hsProvider.requestPersonalSupport(authToken, username)
    }

    //Misc

    fun syncInfo(): SyncInfo {
        return coinSyncer.syncInfo()
    }

    fun getInitialDump(): String {
        return dumpManager.getInitialDump()
    }

    companion object {
        fun getInstance(
            context: Context,
            hsApiBaseUrl: String,
            hsApiKey: String,
            cryptoCompareApiKey: String? = null,
            defiYieldApiKey: String? = null,
            appVersion: String,
            appId: String? = null,
        ): MarketKit {
            // init cache
            (context.getSystemService(Context.STORAGE_SERVICE) as StorageManager?)?.let { storageManager ->
                val cacheDir = context.cacheDir
                val cacheQuotaBytes =
                    storageManager.getCacheQuotaBytes(storageManager.getUuidForPath(cacheDir))

                HSCache.cacheDir = cacheDir
                HSCache.cacheQuotaBytes = cacheQuotaBytes
            }

            val marketDatabase = MarketDatabase.getInstance(context)
            val dumpManager = DumpManager(marketDatabase)
            val hsProvider = HsProvider(hsApiBaseUrl, hsApiKey, appVersion, appId)
            val hsNftProvider = HsNftProvider(hsApiBaseUrl, hsApiKey)
            val coinGeckoProvider = CoinGeckoProvider("https://api.coingecko.com/api/v3/")
            val defiYieldProvider = DefiYieldProvider(defiYieldApiKey)
            val exchangeManager = ExchangeManager(ExchangeStorage(marketDatabase))
            val exchangeSyncer =
                ExchangeSyncer(exchangeManager, coinGeckoProvider, marketDatabase.syncerStateDao())
            val coinStorage = CoinStorage(marketDatabase)
            val coinManager = CoinManager(coinStorage)
            val nftManager = NftManager(coinManager, hsNftProvider)
            val marketOverviewManager = MarketOverviewManager(nftManager, hsProvider)
            val coinSyncer = CoinSyncer(hsProvider, coinStorage, marketDatabase.syncerStateDao())
            val coinPriceManager = CoinPriceManager(CoinPriceStorage(marketDatabase))
            val coinHistoricalPriceManager = CoinHistoricalPriceManager(
                CoinHistoricalPriceStorage(marketDatabase),
                hsProvider,
            )
            val coinPriceSchedulerFactory = CoinPriceSchedulerFactory(coinPriceManager, hsProvider)
            val coinPriceSyncManager = CoinPriceSyncManager(coinPriceSchedulerFactory)
            coinPriceManager.listener = coinPriceSyncManager
            val cryptoCompareProvider = CryptoCompareProvider(cryptoCompareApiKey)
            val postManager = PostManager(cryptoCompareProvider)
            val globalMarketInfoStorage = GlobalMarketInfoStorage(marketDatabase)
            val globalMarketInfoManager = GlobalMarketInfoManager(hsProvider, globalMarketInfoStorage)
            val verifiedExchangeSyncer = VerifiedExchangeSyncer(exchangeManager, hsProvider, marketDatabase.syncerStateDao())
            val hsDataSyncer = HsDataSyncer(coinSyncer, hsProvider, verifiedExchangeSyncer)

            return MarketKit(
                nftManager,
                marketOverviewManager,
                coinManager,
                coinSyncer,
                coinPriceManager,
                coinHistoricalPriceManager,
                coinPriceSyncManager,
                postManager,
                exchangeSyncer,
                globalMarketInfoManager,
                hsProvider,
                hsDataSyncer,
                dumpManager,
                coinGeckoProvider,
                exchangeManager,
                defiYieldProvider,
            )
        }
    }

}

//Errors

sealed class ProviderError : Exception() {
    class ApiRequestLimitExceeded : ProviderError()
    class NoDataForCoin : ProviderError()
    class ReturnedTimestampIsVeryInaccurate : ProviderError()
}

class SyncInfo(
    val coinsTimestamp: String?,
    val blockchainsTimestamp: String?,
    val tokensTimestamp: String?
)

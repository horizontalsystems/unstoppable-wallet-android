package io.horizontalsystems.marketkit

import android.content.Context
import android.os.storage.StorageManager
import io.horizontalsystems.marketkit.chart.HsChartRequestHelper
import io.horizontalsystems.marketkit.managers.CoinHistoricalPriceManager
import io.horizontalsystems.marketkit.managers.CoinManager
import io.horizontalsystems.marketkit.managers.CoinPriceManager
import io.horizontalsystems.marketkit.managers.CoinPriceSyncManager
import io.horizontalsystems.marketkit.managers.DumpManager
import io.horizontalsystems.marketkit.managers.GlobalMarketInfoManager
import io.horizontalsystems.marketkit.managers.MarketOverviewManager
import io.horizontalsystems.marketkit.managers.NftManager
import io.horizontalsystems.marketkit.managers.PostManager
import io.horizontalsystems.marketkit.models.Analytics
import io.horizontalsystems.marketkit.models.AnalyticsPreview
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.ChartPoint
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.CoinCategory
import io.horizontalsystems.marketkit.models.CoinInvestment
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.CoinReport
import io.horizontalsystems.marketkit.models.CoinTreasury
import io.horizontalsystems.marketkit.models.DefiMarketInfo
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.GlobalMarketPoint
import io.horizontalsystems.marketkit.models.HsPeriodType
import io.horizontalsystems.marketkit.models.HsPointTimePeriod
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.horizontalsystems.marketkit.models.MarketInfo
import io.horizontalsystems.marketkit.models.MarketInfoOverview
import io.horizontalsystems.marketkit.models.MarketOverview
import io.horizontalsystems.marketkit.models.MarketTicker
import io.horizontalsystems.marketkit.models.NftTopCollection
import io.horizontalsystems.marketkit.models.Post
import io.horizontalsystems.marketkit.models.RankMultiValue
import io.horizontalsystems.marketkit.models.RankValue
import io.horizontalsystems.marketkit.models.SubscriptionResponse
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenHolders
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TopMovers
import io.horizontalsystems.marketkit.models.TopPair
import io.horizontalsystems.marketkit.models.TopPlatform
import io.horizontalsystems.marketkit.models.TopPlatformMarketCapPoint
import io.horizontalsystems.marketkit.providers.CoinPriceSchedulerFactory
import io.horizontalsystems.marketkit.providers.CryptoCompareProvider
import io.horizontalsystems.marketkit.providers.HsNftProvider
import io.horizontalsystems.marketkit.providers.HsProvider
import io.horizontalsystems.marketkit.storage.CoinHistoricalPriceStorage
import io.horizontalsystems.marketkit.storage.CoinPriceStorage
import io.horizontalsystems.marketkit.storage.CoinStorage
import io.horizontalsystems.marketkit.storage.GlobalMarketInfoStorage
import io.horizontalsystems.marketkit.storage.MarketDatabase
import io.horizontalsystems.marketkit.syncers.CoinSyncer
import io.horizontalsystems.marketkit.syncers.HsDataSyncer
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.Response
import java.math.BigDecimal
import java.util.Date

class MarketKit(
    private val nftManager: NftManager,
    private val marketOverviewManager: MarketOverviewManager,
    private val coinManager: CoinManager,
    private val coinSyncer: CoinSyncer,
    private val coinPriceManager: CoinPriceManager,
    private val coinHistoricalPriceManager: CoinHistoricalPriceManager,
    private val coinPriceSyncManager: CoinPriceSyncManager,
    private val postManager: PostManager,
    private val globalMarketInfoManager: GlobalMarketInfoManager,
    private val hsProvider: HsProvider,
    private val hsDataSyncer: HsDataSyncer,
    private val dumpManager: DumpManager,
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
    ): Single<List<MarketInfo>> {
        return hsProvider.marketInfosSingle(top, currencyCode, defi).map {
            coinManager.getMarketInfos(it)
        }
    }

    fun advancedMarketInfosSingle(
        top: Int = 250,
        currencyCode: String,
    ): Single<List<MarketInfo>> {
        return hsProvider.advancedMarketInfosSingle(top, currencyCode).map {
            coinManager.getMarketInfos(it)
        }
    }

    fun marketInfosSingle(
        coinUids: List<String>,
        currencyCode: String,
    ): Single<List<MarketInfo>> {
        return hsProvider.marketInfosSingle(coinUids, currencyCode).map {
            coinManager.getMarketInfos(it)
        }
    }

    fun marketInfosSingle(
        categoryUid: String,
        currencyCode: String,
    ): Single<List<MarketInfo>> {
        return hsProvider.marketInfosSingle(categoryUid, currencyCode).map {
            coinManager.getMarketInfos(it)
        }
    }

    fun marketInfoOverviewSingle(
        coinUid: String,
        currencyCode: String,
        language: String,
    ): Single<MarketInfoOverview> {
        return hsProvider.getMarketInfoOverview(
            coinUid = coinUid,
            currencyCode = currencyCode,
            language = language,
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

    fun defiMarketInfosSingle(currencyCode: String): Single<List<DefiMarketInfo>> {
        return hsProvider.defiMarketInfosSingle(currencyCode).map {
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
        return hsProvider.marketTickers(coinUid)
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
    ): Single<AnalyticsPreview> {
        return hsProvider.analyticsPreviewSingle(coinUid, addresses)
    }

    fun analyticsSingle(
        authToken: String,
        coinUid: String,
        currencyCode: String,
    ): Single<Analytics> {
        return hsProvider.analyticsSingle(authToken, coinUid, currencyCode)
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

    fun topPairsSingle(currencyCode: String, page: Int, limit: Int): Single<List<TopPair>> =
        hsProvider.topPairsSingle(currencyCode, page, limit)


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
        val data = intervalData(periodType)
        return hsProvider.coinPriceChartSingle(coinUid, currencyCode, data.interval, data.fromTimestamp)
            .map {
                Pair(data.visibleTimestamp, it.map { it.chartPoint })
            }
    }

    private fun intervalData(periodType: HsPeriodType): IntervalData {
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

        return IntervalData(interval, fromTimestamp, visibleTimestamp)
    }

    fun chartStartTimeSingle(coinUid: String): Single<Long> {
        return hsProvider.coinPriceChartStartTime(coinUid)
    }

    fun topPlatformMarketCapStartTimeSingle(platform: String): Single<Long> {
        return hsProvider.topPlatformMarketCapStartTime(platform)
    }

    // Global Market Info

    fun globalMarketPointsSingle(
        currencyCode: String,
        timePeriod: HsTimePeriod
    ): Single<List<GlobalMarketPoint>> {
        return globalMarketInfoManager.globalMarketInfoSingle(currencyCode, timePeriod)
    }

    fun topPlatformsSingle(currencyCode: String): Single<List<TopPlatform>> {
        return hsProvider.topPlatformsSingle(currencyCode)
            .map { responseList -> responseList.map { it.topPlatform } }
    }

    fun topPlatformMarketCapPointsSingle(
        chain: String,
        currencyCode: String,
        periodType: HsPeriodType
    ): Single<List<TopPlatformMarketCapPoint>> {
        val data = intervalData(periodType)
        return hsProvider.topPlatformMarketCapPointsSingle(chain, currencyCode, data.interval, data.fromTimestamp)
    }

    fun topPlatformMarketInfosSingle(
        chain: String,
        currencyCode: String,
    ): Single<List<MarketInfo>> {
        return hsProvider.topPlatformCoinListSingle(chain, currencyCode)
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

    //Stats

    fun sendStats(statsJson: String, appVersion: String, appId: String?): Single<Unit> {
        return hsProvider.sendStats(statsJson, appVersion, appId)
    }

    companion object {
        fun getInstance(
            context: Context,
            hsApiBaseUrl: String,
            hsApiKey: String,
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
            val hsProvider = HsProvider(hsApiBaseUrl, hsApiKey)
            val hsNftProvider = HsNftProvider(hsApiBaseUrl, hsApiKey)
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
            val cryptoCompareProvider = CryptoCompareProvider()
            val postManager = PostManager(cryptoCompareProvider)
            val globalMarketInfoStorage = GlobalMarketInfoStorage(marketDatabase)
            val globalMarketInfoManager = GlobalMarketInfoManager(hsProvider, globalMarketInfoStorage)
            val hsDataSyncer = HsDataSyncer(coinSyncer, hsProvider)

            return MarketKit(
                nftManager,
                marketOverviewManager,
                coinManager,
                coinSyncer,
                coinPriceManager,
                coinHistoricalPriceManager,
                coinPriceSyncManager,
                postManager,
                globalMarketInfoManager,
                hsProvider,
                hsDataSyncer,
                dumpManager,
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

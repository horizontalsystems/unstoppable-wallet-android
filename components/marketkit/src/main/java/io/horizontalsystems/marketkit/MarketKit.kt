package io.horizontalsystems.marketkit

import android.content.Context
import android.os.storage.StorageManager
import io.horizontalsystems.marketkit.chart.ChartManager
import io.horizontalsystems.marketkit.managers.*
import io.horizontalsystems.marketkit.models.*
import io.horizontalsystems.marketkit.providers.*
import io.horizontalsystems.marketkit.storage.*
import io.horizontalsystems.marketkit.syncers.CoinSyncer
import io.horizontalsystems.marketkit.syncers.ExchangeSyncer
import io.horizontalsystems.marketkit.syncers.HsDataSyncer
import io.reactivex.Observable
import io.reactivex.Single
import java.math.BigDecimal

class MarketKit(
    private val nftManager: NftManager,
    private val marketOverviewManager: MarketOverviewManager,
    private val coinManager: CoinManager,
    private val coinSyncer: CoinSyncer,
    private val coinPriceManager: CoinPriceManager,
    private val coinHistoricalPriceManager: CoinHistoricalPriceManager,
    private val coinPriceSyncManager: CoinPriceSyncManager,
    private val postManager: PostManager,
    private val chartManager: ChartManager,
    private val exchangeSyncer: ExchangeSyncer,
    private val globalMarketInfoManager: GlobalMarketInfoManager,
    private val hsProvider: HsProvider,
    private val hsDataSyncer: HsDataSyncer
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

    fun blockchain(uid: String): Blockchain? =
        coinManager.blockchain(uid)

    fun marketInfosSingle(top: Int, currencyCode: String, defi: Boolean = false): Single<List<MarketInfo>> {
        return coinManager.marketInfosSingle(top, currencyCode, defi)
    }

    fun advancedMarketInfosSingle(top: Int = 250, currencyCode: String): Single<List<MarketInfo>> {
        return coinManager.advancedMarketInfosSingle(top, currencyCode)
    }

    fun marketInfosSingle(coinUids: List<String>, currencyCode: String): Single<List<MarketInfo>> {
        return coinManager.marketInfosSingle(coinUids, currencyCode)
    }

    fun marketInfosSingle(categoryUid: String, currencyCode: String): Single<List<MarketInfo>> {
        return coinManager.marketInfosSingle(categoryUid, currencyCode)
    }

    fun marketInfoOverviewSingle(
        coinUid: String,
        currencyCode: String,
        language: String
    ): Single<MarketInfoOverview> {
        return coinManager.marketInfoOverviewSingle(coinUid, currencyCode, language)
    }

    fun marketInfoDetailsSingle(coinUid: String, currencyCode: String): Single<MarketInfoDetails> {
        return coinManager.marketInfoDetailsSingle(coinUid, currencyCode)
    }

    fun marketInfoTvlSingle(
        coinUid: String,
        currencyCode: String,
        timePeriod: HsTimePeriod
    ): Single<List<ChartPoint>> {
        return coinManager.marketInfoTvlSingle(coinUid, currencyCode, timePeriod)
    }

    fun marketInfoGlobalTvlSingle(
        chain: String,
        currencyCode: String,
        timePeriod: HsTimePeriod
    ): Single<List<ChartPoint>> {
        return coinManager.marketInfoGlobalTvlSingle(chain, currencyCode, timePeriod)
    }

    fun defiMarketInfosSingle(currencyCode: String): Single<List<DefiMarketInfo>> {
        return coinManager.defiMarketInfosSingle(currencyCode)
    }

    // Categories

    fun coinCategoriesSingle(currencyCode: String): Single<List<CoinCategory>> =
        hsProvider.getCoinCategories(currencyCode)

    fun coinCategoryMarketPointsSingle(categoryUid: String, interval: HsTimePeriod, currencyCode: String) =
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

    fun coinPriceObservable(coinUid: String, currencyCode: String): Observable<CoinPrice> {
        return coinPriceSyncManager.coinPriceObservable(coinUid, currencyCode)
    }

    fun coinPriceMapObservable(
        coinUids: List<String>,
        currencyCode: String
    ): Observable<Map<String, CoinPrice>> {
        return coinPriceSyncManager.coinPriceMapObservable(coinUids, currencyCode)
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
        return coinManager.marketTickersSingle(coinUid)
    }

    // Details

    fun tokenHoldersSingle(coinUid: String, blockchainUid: String): Single<TokenHolders> {
        return hsProvider.tokenHoldersSingle(coinUid, blockchainUid)
    }

    fun treasuriesSingle(coinUid: String, currencyCode: String): Single<List<CoinTreasury>> {
        return coinManager.treasuriesSingle(coinUid, currencyCode)
    }

    fun investmentsSingle(coinUid: String): Single<List<CoinInvestment>> {
        return coinManager.investmentsSingle(coinUid)
    }

    fun coinReportsSingle(coinUid: String): Single<List<CoinReport>> {
        return coinManager.coinReportsSingle(coinUid)
    }

    fun auditReportsSingle(addresses: List<String>): Single<List<Auditor>> {
        return coinManager.auditReportsSingle(addresses)
    }

    // Pro Data

    fun cexVolumesSingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod): Single<List<ChartPoint>> {
        return hsProvider.coinPriceChartSingle(coinUid, currencyCode, HsPeriodType.ByPeriod(timePeriod))
            .map { response ->
                response.mapNotNull { chartCoinPrice ->
                    chartCoinPrice.totalVolume?.let { volume ->
                        ChartPoint(
                            volume,
                            chartCoinPrice.timestamp,
                            null
                        )
                    }
                }
            }
    }

    fun dexLiquiditySingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod, sessionKey: String?): Single<DexLiquiditiesResponse> {
        return coinManager.dexLiquiditySingle(coinUid, currencyCode, timePeriod, sessionKey)
    }

    fun dexVolumesSingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod, sessionKey: String?): Single<DexVolumesResponse> {
        return coinManager.dexVolumesSingle(coinUid, currencyCode, timePeriod, sessionKey)
    }

    fun transactionDataSingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod, platform: String?, sessionKey: String?): Single<TransactionsDataResponse> {
        return coinManager.transactionDataSingle(coinUid, currencyCode, timePeriod, platform, sessionKey)
    }

    fun activeAddressesSingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod, sessionKey: String?): Single<ActiveAddressesDataResponse> {
        return coinManager.activeAddressesSingle(coinUid, currencyCode, timePeriod, sessionKey)
    }

    fun analyticsPreviewSingle(coinUid: String): Single<AnalyticsPreview> {
        return hsProvider.analyticsPreviewSingle(coinUid)
    }

    fun analyticsSingle(coinUid: String, currencyCode: String): Single<Analytics> {
        return hsProvider.analyticsSingle(coinUid, currencyCode)
    }

    fun cexVolumeRanksSingle(currencyCode: String): Single<List<RankMultiValue>> {
        return hsProvider.rankMultiValueSingle("cex_volume", currencyCode)
    }

    fun dexVolumeRanksSingle(currencyCode: String): Single<List<RankMultiValue>> {
        return hsProvider.rankMultiValueSingle("dex_volume", currencyCode)
    }

    fun dexLiquidityRanksSingle(currencyCode: String): Single<List<RankValue>> {
        return hsProvider.rankValueSingle("dex_liquidity", currencyCode)
    }

    fun activeAddressRanksSingle(currencyCode: String): Single<List<RankMultiValue>> {
        return hsProvider.rankMultiValueSingle("address", currencyCode)
    }

    fun transactionCountsRanksSingle(currencyCode: String): Single<List<RankMultiValue>> {
        return hsProvider.rankMultiValueSingle("tx_count", currencyCode)
    }

    fun revenueRanksSingle(currencyCode: String): Single<List<RankMultiValue>> {
        return hsProvider.rankMultiValueSingle("revenue", currencyCode)
    }

    // Overview
    fun marketOverviewSingle(currencyCode: String): Single<MarketOverview> =
        marketOverviewManager.marketOverviewSingle(currencyCode)


    fun topMoversSingle(currencyCode: String): Single<TopMovers> =
        coinManager.topMoversSingle(currencyCode)

    // Chart Info

    fun chartInfoSingle(coinUid: String, currencyCode: String, periodType: HsPeriodType): Single<List<ChartPoint>> {
        return chartManager.chartInfoSingle(coinUid, currencyCode, periodType)
    }

    fun chartStartTimeSingle(coinUid: String): Single<Long> {
        return chartManager.chartStartTimeSingle(coinUid)
    }

    // Global Market Info

    fun globalMarketPointsSingle(currencyCode: String, timePeriod: HsTimePeriod): Single<List<GlobalMarketPoint>> {
        return globalMarketInfoManager.globalMarketInfoSingle(currencyCode, timePeriod)
    }

    fun topPlatformsSingle(currencyCode: String): Single<List<TopPlatform>> {
        return coinManager.topPlatformsSingle(currencyCode)
    }

    fun topPlatformMarketCapPointsSingle(chain: String, timePeriod: HsTimePeriod, currencyCode: String): Single<List<TopPlatformMarketCapPoint>> {
        return coinManager.topPlatformMarketCapPointsSingle(chain, timePeriod, currencyCode)
    }

    fun topPlatformCoinListSingle(chain: String, currencyCode: String): Single<List<MarketInfo>> {
        return coinManager.topPlatformCoinListSingle(chain, currencyCode)
    }

    // NFT

    suspend fun nftTopCollections(): List<NftTopCollection> = nftManager.topCollections()

    //Misc

    fun syncInfo() : SyncInfo {
        return coinSyncer.syncInfo()
    }

    companion object {
        fun getInstance(
            context: Context,
            hsApiBaseUrl: String,
            hsApiKey: String,
            cryptoCompareApiKey: String? = null,
            defiYieldApiKey: String? = null
        ): MarketKit {
            // init cache
            (context.getSystemService(Context.STORAGE_SERVICE) as StorageManager?)?.let { storageManager ->
                val cacheDir = context.cacheDir
                val cacheQuotaBytes = storageManager.getCacheQuotaBytes(storageManager.getUuidForPath(cacheDir))

                HSCache.cacheDir = cacheDir
                HSCache.cacheQuotaBytes = cacheQuotaBytes
            }

            val marketDatabase = MarketDatabase.getInstance(context)
            val hsProvider = HsProvider(hsApiBaseUrl, hsApiKey)
            val hsNftProvider = HsNftProvider(hsApiBaseUrl, hsApiKey)
            val coinGeckoProvider = CoinGeckoProvider("https://api.coingecko.com/api/v3/")
            val defiYieldProvider = DefiYieldProvider(defiYieldApiKey)
            val exchangeManager = ExchangeManager(ExchangeStorage(marketDatabase))
            val exchangeSyncer = ExchangeSyncer(exchangeManager, coinGeckoProvider, marketDatabase.syncerStateDao())
            val coinStorage = CoinStorage(marketDatabase)
            val coinManager = CoinManager(coinStorage, hsProvider, coinGeckoProvider, defiYieldProvider, exchangeManager)
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
            val chartManager = ChartManager(hsProvider)
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
                chartManager,
                exchangeSyncer,
                globalMarketInfoManager,
                hsProvider,
                hsDataSyncer
            )
        }
    }

}

//Errors

class NoChartData : Exception()
class NoChartInfo : Exception()

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

package io.horizontalsystems.marketkit.providers

import com.google.gson.annotations.SerializedName
import io.horizontalsystems.marketkit.chart.HsChartRequestHelper
import io.horizontalsystems.marketkit.models.*
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.math.BigDecimal
import java.util.*

class HsProvider(baseUrl: String, apiKey: String) {

    private val service by lazy {
        RetrofitUtils.build("${baseUrl}/v1/", mapOf("apikey" to apiKey))
            .create(MarketService::class.java)
    }

    fun marketInfosSingle(
        top: Int,
        currencyCode: String,
        defi: Boolean
    ): Single<List<MarketInfoRaw>> {
        return service.getMarketInfos(top, currencyCode, defi)
    }

    fun advancedMarketInfosSingle(top: Int, currencyCode: String): Single<List<MarketInfoRaw>> {
        return service.getAdvancedMarketInfos(top, currencyCode)
    }

    fun marketInfosSingle(
        coinUids: List<String>,
        currencyCode: String
    ): Single<List<MarketInfoRaw>> {
        return service.getMarketInfos(coinUids.joinToString(","), currencyCode)
    }

    fun marketInfosSingle(categoryUid: String, currencyCode: String): Single<List<MarketInfoRaw>> {
        return service.getMarketInfosByCategory(categoryUid, currencyCode)
    }

    fun getCoinCategories(currencyCode: String): Single<List<CoinCategory>> {
        return service.getCategories(currencyCode)
    }

    fun coinCategoryMarketPointsSingle(
        categoryUid: String,
        timePeriod: HsTimePeriod,
        currencyCode: String,
    ): Single<List<CoinCategoryMarketPoint>> {
        return service.coinCategoryMarketPoints(categoryUid, timePeriod.value, currencyCode)
    }

    fun getCoinPrices(coinUids: List<String>, currencyCode: String): Single<List<CoinPrice>> {
        return service.getCoinPrices(coinUids.joinToString(separator = ","), currencyCode)
            .map { coinPrices ->
                coinPrices.mapNotNull { coinPriceResponse ->
                    coinPriceResponse.coinPrice(currencyCode)
                }
            }
    }

    fun historicalCoinPriceSingle(
        coinUid: String,
        currencyCode: String,
        timestamp: Long
    ): Single<HistoricalCoinPriceResponse> {
        return service.getHistoricalCoinPrice(coinUid, currencyCode, timestamp)
    }

    fun coinPriceChartSingle(
        coinUid: String,
        currencyCode: String,
        periodType: HsPeriodType,
        indicatorPoints: Int
    ): Single<List<ChartCoinPriceResponse>> {
        val currentTime = Date().time / 1000
        val fromTimestamp = HsChartRequestHelper.fromTimestamp(currentTime, periodType, indicatorPoints)
        val pointInterval = HsChartRequestHelper.pointInterval(periodType).value

        return service.getCoinPriceChart(coinUid, currencyCode, fromTimestamp, pointInterval)
    }

    fun coinPriceChartStartTime(coinUid: String): Single<Long> {
        return service.getCoinPriceChartStart(coinUid).map { it.timestamp }
    }

    fun getMarketInfoOverview(
        coinUid: String,
        currencyCode: String,
        language: String,
    ): Single<MarketInfoOverviewRaw> {
        return service.getMarketInfoOverview(coinUid, currencyCode, language)
    }

    fun getGlobalMarketPointsSingle(
        currencyCode: String,
        timePeriod: HsTimePeriod,
    ): Single<List<GlobalMarketPoint>> {
        return service.globalMarketPoints(timePeriod.value, currencyCode)
    }

    fun defiMarketInfosSingle(currencyCode: String): Single<List<DefiMarketInfoResponse>> {
        return service.getDefiMarketInfos(currencyCode)
    }

    fun getMarketInfoDetails(coinUid: String, currency: String): Single<MarketInfoDetailsResponse> {
        return service.getMarketInfoDetails(coinUid, currency)
    }

    fun marketInfoTvlSingle(
        coinUid: String,
        currencyCode: String,
        timePeriod: HsTimePeriod
    ): Single<List<ChartPoint>> {
        return service.getMarketInfoTvl(coinUid, currencyCode, timePeriod.value)
            .map { responseList ->
                responseList.mapNotNull {
                    it.tvl?.let { tvl -> ChartPoint(tvl, it.timestamp, emptyMap()) }
                }
            }
    }

    fun marketInfoGlobalTvlSingle(
        chain: String,
        currencyCode: String,
        timePeriod: HsTimePeriod
    ): Single<List<ChartPoint>> {

        return service.getMarketInfoGlobalTvl(
            currencyCode,
            timePeriod.value,
            blockchain = if (chain.isNotBlank()) chain else null
        ).map { responseList ->
            responseList.mapNotNull {
                it.tvl?.let { tvl ->
                    ChartPoint(tvl, it.timestamp, emptyMap())
                }
            }
        }
    }

    fun topHoldersSingle(coinUid: String): Single<List<TokenHolder>> {
        return service.getTopHolders(coinUid)
    }

    fun coinTreasuriesSingle(coinUid: String, currencyCode: String): Single<List<CoinTreasury>> {
        return service.getCoinTreasuries(coinUid, currencyCode).map { responseList ->
            responseList.mapNotNull {
                try {
                    CoinTreasury(
                        type = CoinTreasury.TreasuryType.fromString(it.type)!!,
                        fund = it.fund,
                        fundUid = it.fundUid,
                        amount = it.amount,
                        amountInCurrency = it.amountInCurrency,
                        countryCode = it.countryCode
                    )
                } catch (exception: Exception) {
                    null
                }
            }
        }
    }

    fun investmentsSingle(coinUid: String): Single<List<CoinInvestment>> {
        return service.getInvestments(coinUid)
    }

    fun coinReportsSingle(coinUid: String): Single<List<CoinReport>> {
        return service.getCoinReports(coinUid)
    }

    fun topPlatformsSingle(currencyCode: String): Single<List<TopPlatformResponse>> {
        return service.getTopPlatforms(currencyCode)
    }

    fun topPlatformMarketCapPointsSingle(chain: String, timePeriod: HsTimePeriod, currencyCode: String): Single<List<TopPlatformMarketCapPoint>> {
        return service.getTopPlatformMarketCapPoints(chain, timePeriod.value, currencyCode)
    }

    fun topPlatformCoinListSingle(chain: String, currencyCode: String): Single<List<MarketInfoRaw>> {
        return service.getTopPlatformCoinList(chain, currencyCode)
    }

    fun dexLiquiditySingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod, sessionKey: String?): Single<DexLiquiditiesResponse> {
        return service.getDexLiquidities(sessionKey?.let { "Bearer ${it}" }, coinUid, timePeriod.value)
    }

    fun dexVolumesSingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod, sessionKey: String?): Single<DexVolumesResponse> {
        return service.getDexVolumes(sessionKey?.let { "Bearer ${it}" }, coinUid, timePeriod.value)
    }

    fun transactionDataSingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod, platform: String?, sessionKey: String?): Single<TransactionsDataResponse> {
        return service.getTransactions(sessionKey?.let { "Bearer ${it}" }, coinUid, timePeriod.value, platform)
    }

    fun activeAddressesSingle(coinUid: String, currencyCode: String, timePeriod: HsTimePeriod, sessionKey: String?): Single<ActiveAddressesDataResponse> {
        return service.getActiveAddresses(sessionKey?.let { "Bearer ${it}" }, coinUid, timePeriod.value)
    }

    fun marketOverviewSingle(currencyCode: String): Single<MarketOverviewResponse> {
        return service.getMarketOverview(currencyCode)
    }

    fun topMoversRawSingle(currencyCode: String): Single<TopMoversRaw> {
        return service.getTopMovers(currencyCode)
    }

    fun statusSingle(): Single<HsStatus> {
        return service.getStatus()
    }

    fun allCoinsSingle(): Single<List<CoinResponse>> {
        return service.getAllCoins()
    }

    fun allBlockchainsSingle(): Single<List<BlockchainResponse>> {
        return service.getAllBlockchains()
    }

    fun allTokensSingle(): Single<List<TokenResponse>> {
        return service.getAllTokens()
    }

    private interface MarketService {

        @GET("coins")
        fun getMarketInfos(
            @Query("limit") top: Int,
            @Query("currency") currencyCode: String,
            @Query("defi") defi: Boolean,
            @Query("order_by_rank") orderByRank: Boolean = true,
            @Query("fields") fields: String = marketInfoFields,
        ): Single<List<MarketInfoRaw>>

        @GET("coins")
        fun getAdvancedMarketInfos(
            @Query("limit") top: Int,
            @Query("currency") currencyCode: String,
            @Query("order_by_rank") orderByRank: Boolean = true,
            @Query("fields") fields: String = advancedMarketFields,
        ): Single<List<MarketInfoRaw>>

        @GET("coins")
        fun getMarketInfos(
            @Query("uids") uids: String,
            @Query("currency") currencyCode: String,
            @Query("fields") fields: String = marketInfoFields,
        ): Single<List<MarketInfoRaw>>

        @GET("categories/{categoryUid}/coins")
        fun getMarketInfosByCategory(
            @Path("categoryUid") categoryUid: String,
            @Query("currency") currencyCode: String,
        ): Single<List<MarketInfoRaw>>

        @GET("categories")
        fun getCategories(
            @Query("currency") currencyCode: String
        ): Single<List<CoinCategory>>

        @GET("categories/{categoryUid}/market_cap")
        fun coinCategoryMarketPoints(
            @Path("categoryUid") categoryUid: String,
            @Query("interval") interval: String,
            @Query("currency") currencyCode: String,
        ): Single<List<CoinCategoryMarketPoint>>

        @GET("coins")
        fun getCoinPrices(
            @Query("uids") uids: String,
            @Query("currency") currencyCode: String,
            @Query("fields") fields: String = coinPriceFields,
        ): Single<List<CoinPriceResponse>>

        @GET("coins/{coinUid}/price_history")
        fun getHistoricalCoinPrice(
            @Path("coinUid") coinUid: String,
            @Query("currency") currencyCode: String,
            @Query("timestamp") timestamp: Long,
        ): Single<HistoricalCoinPriceResponse>

        @GET("coins/{coinUid}/price_chart")
        fun getCoinPriceChart(
            @Path("coinUid") coinUid: String,
            @Query("currency") currencyCode: String,
            @Query("from_timestamp") timestamp: Long?,
            @Query("interval") interval: String,
        ): Single<List<ChartCoinPriceResponse>>

        @GET("coins/{coinUid}/price_chart_start")
        fun getCoinPriceChartStart(
            @Path("coinUid") coinUid: String
        ): Single<PriceChartStart>

        @GET("coins/{coinUid}")
        fun getMarketInfoOverview(
            @Path("coinUid") coinUid: String,
            @Query("currency") currencyCode: String,
            @Query("language") language: String,
        ): Single<MarketInfoOverviewRaw>

        @GET("defi-protocols")
        fun getDefiMarketInfos(
            @Query("currency") currencyCode: String
        ): Single<List<DefiMarketInfoResponse>>

        @GET("coins/{coinUid}/details")
        fun getMarketInfoDetails(
            @Path("coinUid") coinUid: String,
            @Query("currency") currencyCode: String
        ): Single<MarketInfoDetailsResponse>

        @GET("transactions/dex-liquidity")
        fun getDexLiquidities(
            @Header("authorization") auth: String?,
            @Query("coin_uid") coinUid: String,
            @Query("interval") interval: String
        ): Single<DexLiquiditiesResponse>

        @GET("transactions/dex-volumes")
        fun getDexVolumes(
            @Header("authorization") auth: String?,
            @Query("coin_uid") coinUid: String,
            @Query("interval") interval: String
        ): Single<DexVolumesResponse>

        @GET("transactions")
        fun getTransactions(
            @Header("authorization") auth: String?,
            @Query("coin_uid") coinUid: String,
            @Query("interval") interval: String,
            @Query("platform") platform: String?
        ): Single<TransactionsDataResponse>

        @GET("addresses")
        fun getActiveAddresses(
            @Header("authorization") auth: String?,
            @Query("coin_uid") coinUid: String,
            @Query("interval") interval: String
        ): Single<ActiveAddressesDataResponse>

        @GET("defi-protocols/{coinUid}/tvls")
        fun getMarketInfoTvl(
            @Path("coinUid") coinUid: String,
            @Query("currency") currencyCode: String,
            @Query("interval") interval: String
        ): Single<List<MarketInfoTvlResponse>>

        @GET("global-markets/tvls")
        fun getMarketInfoGlobalTvl(
            @Query("currency") currencyCode: String,
            @Query("interval") interval: String,
            @Query("blockchain") blockchain: String?
        ): Single<List<MarketInfoTvlResponse>>

        @GET("addresses/holders")
        fun getTopHolders(
            @Query("coin_uid") coinUid: String
        ): Single<List<TokenHolder>>

        @GET("funds/treasuries")
        fun getCoinTreasuries(
            @Query("coin_uid") coinUid: String,
            @Query("currency") currencyCode: String
        ): Single<List<CoinTreasuryResponse>>

        @GET("funds/investments")
        fun getInvestments(
            @Query("coin_uid") coinUid: String,
        ): Single<List<CoinInvestment>>

        @GET("reports")
        fun getCoinReports(
            @Query("coin_uid") coinUid: String
        ): Single<List<CoinReport>>

        @GET("global-markets")
        fun globalMarketPoints(
            @Query("interval") timePeriod: String,
            @Query("currency") currencyCode: String,
        ): Single<List<GlobalMarketPoint>>

        @GET("top-platforms")
        fun getTopPlatforms(
            @Query("currency") currencyCode: String
        ): Single<List<TopPlatformResponse>>

        @GET("top-platforms/{chain}/chart")
        fun getTopPlatformMarketCapPoints(
            @Path("chain") chain: String,
            @Query("interval") timePeriod: String,
            @Query("currency") currencyCode: String,
        ): Single<List<TopPlatformMarketCapPoint>>

        @GET("top-platforms/{chain}/list")
        fun getTopPlatformCoinList(
            @Path("chain") chain: String,
            @Query("currency") currencyCode: String,
        ): Single<List<MarketInfoRaw>>

        @GET("markets/overview")
        fun getMarketOverview(
            @Query("currency") currencyCode: String,
            @Query("simplified") simplified: Boolean = true
        ): Single<MarketOverviewResponse>

        @GET("coins/top-movers")
        fun getTopMovers(
            @Query("currency") currencyCode: String
        ): Single<TopMoversRaw>

        @GET("status/updates")
        fun getStatus(): Single<HsStatus>

        @GET("coins/list")
        fun getAllCoins(): Single<List<CoinResponse>>

        @GET("blockchains/list")
        fun getAllBlockchains(): Single<List<BlockchainResponse>>

        @GET("tokens/list")
        fun getAllTokens(): Single<List<TokenResponse>>

        companion object {
            private const val marketInfoFields =
                "name,code,price,price_change_24h,market_cap_rank,coingecko_id,market_cap,market_cap_rank,total_volume"
            private const val coinPriceFields = "price,price_change_24h,last_updated"
            private const val advancedMarketFields =
                "all_platforms,price,market_cap,total_volume,price_change_24h,price_change_7d,price_change_14d,price_change_30d,price_change_200d,price_change_1y,ath_percentage,atl_percentage"
        }
    }
}

data class HistoricalCoinPriceResponse(
    val timestamp: Long,
    val price: BigDecimal,
)

data class PriceChartStart(val timestamp: Long)

data class ChartCoinPriceResponse(
    val timestamp: Long,
    val price: BigDecimal,
    @SerializedName("volume")
    val totalVolume: BigDecimal?
) {
    val chartPoint: ChartPoint
        get() {
            return ChartPoint(
                price,
                timestamp,
                totalVolume?.let { mapOf(ChartPointType.Volume to it) } ?: emptyMap()
            )
        }
}

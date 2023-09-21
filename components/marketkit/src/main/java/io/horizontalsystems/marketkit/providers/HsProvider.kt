package io.horizontalsystems.marketkit.providers

import com.google.gson.annotations.SerializedName
import io.horizontalsystems.marketkit.models.*
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap
import java.math.BigDecimal
import java.util.*

class HsProvider(baseUrl: String, apiKey: String, appVersion: String, appId: String?) {

    // TODO Remove old base URL https://api-dev.blocksdecoded.com/v1 and switch it to new servers
    private val newBaseUrl: String = "https://p.cash"

    private val service by lazy {
        val headerMap = mutableMapOf<String, String>()
        headerMap["app_platform"] = "android"
        headerMap["app_version"] = appVersion
        appId?.let {
            headerMap["app_id"] = it
        }
        headerMap["apikey"] = apiKey

        RetrofitUtils.build("${newBaseUrl}/s1/", headerMap)
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

    fun getCoinPrices(
        coinUids: List<String>,
        walletCoinUids: List<String>,
        currencyCode: String
    ): Single<List<CoinPrice>> {
        val additionalParams = mutableMapOf<String, String>()
        if (walletCoinUids.isNotEmpty()) {
            additionalParams["enabled_uids"] = walletCoinUids.joinToString(separator = ",")
        }
        return service.getCoinPrices(
            uids = coinUids.joinToString(separator = ","),
            currencyCode = currencyCode,
            additionalParams = additionalParams
        )
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
        periodType: HsPointTimePeriod,
        fromTimestamp: Long?
    ): Single<List<ChartCoinPriceResponse>> {
        return service.getCoinPriceChart(coinUid, currencyCode, fromTimestamp, periodType.value)
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
                    it.tvl?.let { tvl -> ChartPoint(tvl, it.timestamp, null) }
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
                    ChartPoint(tvl, it.timestamp, null)
                }
            }
        }
    }

    fun tokenHoldersSingle(
        authToken: String,
        coinUid: String,
        blockchainUid: String
    ): Single<TokenHolders> {
        return service.getTokenHolders(authToken, coinUid, blockchainUid)
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

    fun dexLiquiditySingle(
        authToken: String,
        coinUid: String,
        currencyCode: String,
        timePeriod: HsTimePeriod
    ): Single<List<Analytics.VolumePoint>> {
        return service.getDexLiquidities(authToken, coinUid, currencyCode, timePeriod.value)
    }

    fun dexVolumesSingle(
        authToken: String,
        coinUid: String,
        currencyCode: String,
        timePeriod: HsTimePeriod
    ): Single<List<Analytics.VolumePoint>> {
        return service.getDexVolumes(authToken, coinUid, currencyCode, timePeriod.value)
    }

    fun transactionDataSingle(
        authToken: String,
        coinUid: String,
        timePeriod: HsTimePeriod,
        platform: String?
    ): Single<List<Analytics.CountVolumePoint>> {
        return service.getTransactions(authToken, coinUid, timePeriod.value, platform)
    }

    fun activeAddressesSingle(
        authToken: String,
        coinUid: String,
        timePeriod: HsTimePeriod
    ): Single<List<Analytics.CountPoint>> {
        return service.getActiveAddresses(authToken, coinUid, timePeriod.value)
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

    fun analyticsPreviewSingle(coinUid: String, addresses: List<String>): Single<AnalyticsPreview> {
        return service.getAnalyticsPreview(coinUid, if (addresses.isEmpty()) null else addresses.joinToString(","))
    }

    fun analyticsSingle(
        authToken: String,
        coinUid: String,
        currencyCode: String
    ): Single<Analytics> {
        return service.getAnalyticsData(authToken, coinUid, currencyCode)
    }

    fun rankValueSingle(
        authToken: String,
        type: String,
        currencyCode: String
    ): Single<List<RankValue>> {
        return service.getRankValue(authToken, type, currencyCode)
    }

    fun rankMultiValueSingle(
        authToken: String,
        type: String,
        currencyCode: String
    ): Single<List<RankMultiValue>> {
        return service.getRankMultiValue(authToken, type, currencyCode)
    }

    fun subscriptionsSingle(
        addresses: List<String>
    ): Single<List<SubscriptionResponse>> {
        return service.getSubscriptions(addresses.joinToString(separator = ","))
    }

    fun authGetSignMessage(address: String): Single<String> {
        return service.authGetSignMessage(address)
            .map { it["message"] }
    }

    fun authenticate(signature: String, address: String): Single<String> {
        return service.authenticate(signature, address)
            .map { it["token"] }
    }

    fun requestPersonalSupport(authToken: String, username: String): Single<Response<Void>> {
        return service.requestPersonalSupport(authToken, username)
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
            @QueryMap additionalParams: Map<String, String>,
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

        @GET("analytics/{coinUid}/preview")
        fun getAnalyticsPreview(
            @Path("coinUid") coinUid: String,
            @Query("address") address: String?,
        ): Single<AnalyticsPreview>

        @GET("analytics/{coinUid}")
        fun getAnalyticsData(
            @Header("authorization") authToken: String,
            @Path("coinUid") coinUid: String,
            @Query("currency") currencyCode: String,
        ): Single<Analytics>

        @GET("analytics/{coinUid}/dex-liquidity")
        fun getDexLiquidities(
            @Header("authorization") auth: String,
            @Path("coinUid") coinUid: String,
            @Query("currency") currencyCode: String,
            @Query("interval") interval: String,
        ): Single<List<Analytics.VolumePoint>>

        @GET("analytics/{coinUid}/dex-volumes")
        fun getDexVolumes(
            @Header("authorization") auth: String,
            @Path("coinUid") coinUid: String,
            @Query("currency") currencyCode: String,
            @Query("interval") interval: String
        ): Single<List<Analytics.VolumePoint>>

        @GET("analytics/{coinUid}/transactions")
        fun getTransactions(
            @Header("authorization") auth: String,
            @Path("coinUid") coinUid: String,
            @Query("interval") interval: String,
            @Query("platform") platform: String?
        ): Single<List<Analytics.CountVolumePoint>>

        @GET("analytics/{coinUid}/addresses")
        fun getActiveAddresses(
            @Header("authorization") auth: String,
            @Path("coinUid") coinUid: String,
            @Query("interval") interval: String
        ): Single<List<Analytics.CountPoint>>

        @GET("analytics/{coinUid}/holders")
        fun getTokenHolders(
            @Header("authorization") authToken: String,
            @Path("coinUid") coinUid: String,
            @Query("blockchain_uid") blockchainUid: String
        ): Single<TokenHolders>

        @GET("analytics/ranks")
        fun getRankValue(
            @Header("authorization") authToken: String,
            @Query("type") type: String,
            @Query("currency") currencyCode: String,
        ): Single<List<RankValue>>

        @GET("analytics/ranks")
        fun getRankMultiValue(
            @Header("authorization") authToken: String,
            @Query("type") type: String,
            @Query("currency") currencyCode: String,
        ): Single<List<RankMultiValue>>

        @GET("analytics/subscriptions")
        fun getSubscriptions(
            @Query("address") addresses: String
        ): Single<List<SubscriptionResponse>>

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

        @GET("auth/get-sign-message")
        fun authGetSignMessage(
            @Query("address") address: String
        ): Single<Map<String, String>>

        @FormUrlEncoded
        @POST("auth/authenticate")
        fun authenticate(
            @Field("signature") signature: String,
            @Field("address") address: String
        ): Single<Map<String, String>>

        @FormUrlEncoded
        @POST("support/start-chat")
        fun requestPersonalSupport(
            @Header("authorization") auth: String,
            @Field("username") username: String,
        ): Single<Response<Void>>


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
                totalVolume
            )
        }
}

package cash.p.terminal.wallet.providers

import android.util.Log
import cash.p.terminal.network.data.entity.ChartPeriod
import cash.p.terminal.network.pirate.domain.repository.PiratePlaceRepository
import cash.p.terminal.wallet.models.Analytics
import cash.p.terminal.wallet.models.AnalyticsPreview
import cash.p.terminal.wallet.models.BlockchainResponse
import cash.p.terminal.wallet.models.ChartPoint
import cash.p.terminal.wallet.models.CoinCategory
import cash.p.terminal.wallet.models.CoinCategoryMarketPoint
import cash.p.terminal.wallet.models.CoinInvestment
import cash.p.terminal.wallet.models.CoinPrice
import cash.p.terminal.wallet.models.CoinPriceResponse
import cash.p.terminal.wallet.models.CoinReport
import cash.p.terminal.wallet.models.CoinResponse
import cash.p.terminal.wallet.models.CoinTreasury
import cash.p.terminal.wallet.models.CoinTreasuryResponse
import cash.p.terminal.wallet.models.DefiMarketInfoResponse
import cash.p.terminal.wallet.models.EtfPointResponse
import cash.p.terminal.wallet.models.EtfResponse
import cash.p.terminal.wallet.models.GlobalMarketPoint
import cash.p.terminal.wallet.models.HsPointTimePeriod
import cash.p.terminal.wallet.models.HsStatus
import cash.p.terminal.wallet.models.MarketGlobal
import cash.p.terminal.wallet.models.MarketInfoDetailsResponse
import cash.p.terminal.wallet.models.MarketInfoOverviewRaw
import cash.p.terminal.wallet.models.MarketInfoRaw
import cash.p.terminal.wallet.models.MarketInfoTvlResponse
import cash.p.terminal.wallet.models.MarketOverviewResponse
import cash.p.terminal.wallet.models.MarketTicker
import cash.p.terminal.wallet.models.PiratePlaceCoinRaw
import cash.p.terminal.wallet.models.RankMultiValue
import cash.p.terminal.wallet.models.RankValue
import cash.p.terminal.wallet.models.SubscriptionResponse
import cash.p.terminal.wallet.models.TokenHolders
import cash.p.terminal.wallet.models.TokenResponse
import cash.p.terminal.wallet.models.TopMoversRaw
import cash.p.terminal.wallet.models.TopPair
import cash.p.terminal.wallet.models.TopPlatformMarketCapPoint
import cash.p.terminal.wallet.models.TopPlatformResponse
import cash.p.terminal.wallet.providers.mapper.PirateCoinInfoMapper
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.core.models.HsTimePeriod
import io.reactivex.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap
import java.math.BigDecimal
import java.time.Instant

class HsProvider(baseUrl: String, apiKey: String) {

    private val piratePlaceRepository: PiratePlaceRepository by inject(PiratePlaceRepository::class.java)
    private val pirateCoinInfoMapper: PirateCoinInfoMapper by inject(PirateCoinInfoMapper::class.java)

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // TODO Remove old base URL https://api-dev.blocksdecoded.com/v1 and switch it to new servers
    private val pirateService by lazy {
        RetrofitUtils.build("https://pirate.cash/s1/", mapOf("apikey" to apiKey))
            .create(MarketService::class.java)
    }

    private val piratePlaceService by lazy {
        RetrofitUtils.build("https://pirate.place/api/")
            .create(MarketService::class.java)
    }

    private val service by lazy {
        RetrofitUtils.build("${baseUrl}/v1/", mapOf("apikey" to apiKey))
            .create(MarketService::class.java)
    }

    fun marketInfosSingle(
        top: Int,
        currencyCode: String,
        defi: Boolean,
    ): Single<List<MarketInfoRaw>> {
        return service.getMarketInfos(
            top = top,
            currencyCode = currencyCode,
            defi = defi
        )
    }

    fun advancedMarketInfosSingle(
        top: Int,
        currencyCode: String,
    ): Single<List<MarketInfoRaw>> {
        return service.getAdvancedMarketInfos(
            top = top,
            currencyCode = currencyCode
        )
    }

    fun marketInfosSingle(
        coinUids: List<String>,
        currencyCode: String,
    ): Single<List<MarketInfoRaw>> {
        return service.getMarketInfos(
            uids = coinUids.joinToString(","),
            currencyCode = currencyCode
        )
    }

    fun topCoinsMarketInfosSingle(
        top: Int,
        currencyCode: String,
    ): Single<List<MarketInfoRaw>> {
        return service.getTopCoinsMarketInfos(
            top = top,
            currencyCode = currencyCode,
        )
    }

    fun marketInfosSingle(
        categoryUid: String,
        currencyCode: String,
    ): Single<List<MarketInfoRaw>> {
        return service.getMarketInfosByCategory(
            categoryUid = categoryUid,
            currencyCode = currencyCode
        )
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

    private suspend fun fetchPlaceCoinPrices(
        requestUid: Set<String>,
        currencyCode: String,
    ): List<CoinPriceResponse> {
        val currency = currencyCode.lowercase()
        return requestUid.map { coroutineScope.async { runCatching { fetchPiratePlaceCoinInfo(it) }.getOrNull() } }
            .awaitAll()
            .filterNotNull().mapNotNull {
                val price = when (currency) {
                    "usd" -> it.price.usd
                    "btc" -> it.price.btc
                    "eur" -> it.price.eur
                    "gbp" -> it.price.gbp
                    "jpy" -> it.price.jpy
                    "aud" -> it.price.aud
                    "ars" -> it.price.aud
                    "brl" -> it.price.brl
                    "cad" -> it.price.cad
                    "chf" -> it.price.chf
                    "cny" -> it.price.cny
                    "hkd" -> it.price.hkd
                    "huf" -> it.price.hkd
                    "ils" -> it.price.ils
                    "inr" -> it.price.inr
                    "nok" -> it.price.inr
                    "php" -> it.price.inr
                    "rub" -> it.price.rub
                    "sgd" -> it.price.sgd
                    "zar" -> it.price.zar
                    else -> null
                }
                val priceChange = it.changes.price.percentage24h[currency]
                if (price != null && priceChange != null) {
                    CoinPriceResponse(
                        uid = it.id,
                        price = price,
                        priceChange24h = priceChange,
                        priceChange1d = priceChange,
                        lastUpdated = Instant.now().epochSecond
                    )
                } else {
                    null
                }
            }
    }

    suspend fun getCoinPrices(
        coinUids: List<String>,
        walletCoinUids: List<String>,
        currencyCode: String
    ): List<CoinPrice> {
        val additionalParams = mutableMapOf<String, String>()
        if (walletCoinUids.isNotEmpty()) {
            additionalParams["enabled_uids"] = walletCoinUids.joinToString(separator = ",")
        }
        val fetchedPrices = service.getCoinPrices(
            uids = coinUids.joinToString(separator = ","),
            currencyCode = currencyCode,
            additionalParams = additionalParams
        ).await()

        return if (fetchedPrices.size != coinUids.size) {
            val pricesNotFound =
                coinUids.toMutableSet().apply {
                    removeAll(fetchedPrices.map { it.uid }.toSet())
                }
            // Similar coins with different uids
            if (pricesNotFound.remove("wdash")) {
                pricesNotFound.add("dash")
            }
            Log.d("HsProvider", "Prices not found: $pricesNotFound, requesting on PiratePlace")
            fetchedPrices + fetchPlaceCoinPrices(
                requestUid = pricesNotFound,
                currencyCode = currencyCode
            ).also { coinPrices ->
                if (coinPrices.size != pricesNotFound.size) {
                    val pricesNotFound2 =
                        pricesNotFound.toMutableSet().apply {
                            removeAll(coinPrices.map { it.uid }.toSet())
                        }
                    Log.d("HsProvider", "Prices NOT FOUND on PiratePlace: $pricesNotFound2")
                }
            }
        } else {
            fetchedPrices
        }.mapNotNull { coinPriceResponse ->
            coinPriceResponse.coinPrice(
                currencyCode = currencyCode,
                alternativeUid = if (coinPriceResponse.uid == "dash") "wdash" else null
            )
        }
    }

    private suspend fun fetchPiratePlaceCoinInfo(
        uid: String
    ): PiratePlaceCoinRaw = withContext(Dispatchers.IO) {
        piratePlaceService.getPlaceCoinInfo(coin = uid).await()
    }

    fun historicalCoinPriceSingle(
        coinUid: String,
        currencyCode: String,
        timestamp: Long
    ): Single<HistoricalCoinPriceResponse> {
        return service.getHistoricalCoinPrice(coinUid, currencyCode, timestamp)
    }

    suspend fun coinPriceChartSingle(
        coinUid: String,
        currencyCode: String,
        periodType: HsTimePeriod?,
        pointPeriodType: HsPointTimePeriod,
        fromTimestamp: Long?
    ): List<ChartCoinPriceResponse> {
        var cartPrices =
            service.getCoinPriceChart(coinUid, currencyCode, fromTimestamp, pointPeriodType.value)
                .await()
        if (cartPrices.isEmpty()) {
            val period = if (fromTimestamp == null) {
                ChartPeriod.MAX
            } else {
                when (periodType) {
                    HsTimePeriod.Hour1 -> ChartPeriod.HOUR
                    HsTimePeriod.Day1 -> ChartPeriod.DAY
                    HsTimePeriod.Week1 -> ChartPeriod.WEEK
                    HsTimePeriod.Month1 -> ChartPeriod.MONTH
                    HsTimePeriod.Year1 -> ChartPeriod.YEAR
                    else -> ChartPeriod.MONTH
                }
            }
            cartPrices = piratePlaceRepository.getCoinPriceChart(
                coin = coinUid,
                periodType = period
            ).map {
                ChartCoinPriceResponse(
                    price = it.price,
                    timestamp = it.timestamp / 1000,
                    totalVolume = BigDecimal.ZERO
                )
            }
        }
        return cartPrices
    }

    fun coinPriceChartStartTime(coinUid: String): Single<Long> {
        return service.getCoinPriceChartStart(coinUid).map { it.timestamp }
    }

    fun topPlatformMarketCapStartTime(platform: String): Single<Long> {
        return service.getTopPlatformMarketCapStart(platform).map { it.timestamp }
    }

    suspend fun getMarketInfoOverview(
        coinUid: String,
        currencyCode: String,
        language: String,
    ): MarketInfoOverviewRaw {
        return try {
            service.getMarketInfoOverview(
                coinUid = coinUid,
                currencyCode = currencyCode,
                language = language
            ).await()
        } catch (e: Exception) {
            piratePlaceRepository.getCoinInfo(coinUid).let(pirateCoinInfoMapper::mapCoinInfo)
        }
    }

    fun getGlobalMarketPointsSingle(
        currencyCode: String,
        timePeriod: HsTimePeriod,
    ): Single<List<GlobalMarketPoint>> {
        return service.globalMarketPoints(timePeriod.value, currencyCode)
    }

    fun defiMarketInfosSingle(currencyCode: String): Single<List<DefiMarketInfoResponse>> {
        return service.getDefiMarketInfos(currencyCode = currencyCode)
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
        return service.getTopPlatforms(currencyCode = currencyCode)
    }

    fun topPlatformMarketCapPointsSingle(
        chain: String,
        currencyCode: String,
        periodType: HsPointTimePeriod,
        fromTimestamp: Long?
    ): Single<List<TopPlatformMarketCapPoint>> {
        return service.getTopPlatformMarketCapPoints(
            chain,
            currencyCode,
            fromTimestamp,
            periodType.value
        )
    }

    fun topPlatformCoinListSingle(
        chain: String,
        currencyCode: String
    ): Single<List<MarketInfoRaw>> {
        return service.getTopPlatformCoinList(
            chain = chain,
            currencyCode = currencyCode
        )
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

    fun marketGlobalSingle(currencyCode: String): Single<MarketGlobal> {
        return service.getMarketGlobal(currencyCode)
    }

    suspend fun marketTickers(coinUid: String, currencyCode: String): List<MarketTicker> {
        return try {
            service.getMarketTickers(coinUid, currencyCode).await()
        } catch (e: Exception) {
            piratePlaceRepository.getMarketTickers(coinUid).map {
                MarketTicker(
                    base = it.fromSymbol,
                    target = "USD",
                    marketName = it.market,
                    marketImageUrl = null,
                    volume = it.volume,
                    fiatVolume = it.volumeUsd,
                    tradeUrl = it.tradeUrl,
                    verified = false
                )
            }
        }
    }

    fun topMoversRawSingle(currencyCode: String): Single<TopMoversRaw> {
        return service.getTopMovers(currencyCode)
    }

    fun statusSingle(): Single<HsStatus> {
        return pirateService.getStatus()
    }

    fun allCoinsSingle(): Single<List<CoinResponse>> {
        return pirateService.getAllCoins()
    }

    fun allBlockchainsSingle(): Single<List<BlockchainResponse>> {
        return pirateService.getAllBlockchains()
    }

    fun allTokensSingle(): Single<List<TokenResponse>> {
        return pirateService.getAllTokens()
    }

    fun analyticsPreviewSingle(coinUid: String, addresses: List<String>): Single<AnalyticsPreview> {
        return service.getAnalyticsPreview(
            coinUid = coinUid,
            address = if (addresses.isEmpty()) null else addresses.joinToString(",")
        )
    }

    fun analyticsSingle(
        authToken: String,
        coinUid: String,
        currencyCode: String,
    ): Single<Analytics> {
        return service.getAnalyticsData(
            authToken = authToken,
            coinUid = coinUid,
            currencyCode = currencyCode
        )
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

    fun verifiedExchangeUids(): Single<List<String>> {
        return service.verifiedExchangeUids()
    }

    fun topPairsSingle(currencyCode: String, page: Int, limit: Int): Single<List<TopPair>> {
        return service.getTopPairs(currencyCode, page, limit)
    }

    fun sendStats(statsJson: String, appVersion: String, appId: String?): Single<Unit> {
        return service.sendStats(
            appPlatform = "android",
            appVersion = appVersion,
            appId = appId,
            stats = statsJson
        )
    }

    fun coinsSignalsSingle(uids: List<String>): Single<List<SignalResponse>> {
        return service.getCoinsSignals(uids.joinToString(separator = ","))
    }

    fun etfsSingle(currencyCode: String): Single<List<EtfResponse>> {
        return service.getEtfs(currencyCode)
    }

    fun etfPointsSingle(currencyCode: String): Single<List<EtfPointResponse>> {
        return service.getEtfPoints(currencyCode)
    }

    private interface MarketService {
        @GET("coins/{coin}")
        fun getPlaceCoinInfo(@Path("coin") coin: String): Single<PiratePlaceCoinRaw>

        @GET("coins")
        fun getMarketInfos(
            @Query("limit") top: Int,
            @Query("currency") currencyCode: String,
            @Query("defi") defi: Boolean,
            @Query("order_by_rank") orderByRank: Boolean = true,
            @Query("fields") fields: String = marketInfoFields,
        ): Single<List<MarketInfoRaw>>

        @GET("coins")
        fun getTopCoinsMarketInfos(
            @Query("limit") top: Int,
            @Query("currency") currencyCode: String,
            @Query("order_by_rank") orderByRank: Boolean = true,
            @Query("fields") fields: String = topCoinsMarketInfoFields,
        ): Single<List<MarketInfoRaw>>

        @GET("coins/filter")
        fun getAdvancedMarketInfos(
            @Query("limit") top: Int,
            @Query("currency") currencyCode: String,
            @Query("order_by_rank") orderByRank: Boolean = true,
            @Query("page") page: Int = 1,
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
        ): Single<ChartStart>

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

        @GET("top-platforms/{platform}/market_chart_start")
        fun getTopPlatformMarketCapStart(
            @Path("platform") platform: String
        ): Single<ChartStart>

        @GET("top-platforms/{platform}/market_chart")
        fun getTopPlatformMarketCapPoints(
            @Path("platform") platform: String,
            @Query("currency") currencyCode: String,
            @Query("from_timestamp") timestamp: Long?,
            @Query("interval") interval: String
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

        @GET("markets/overview-simple")
        fun getMarketGlobal(
            @Query("currency") currencyCode: String
        ): Single<MarketGlobal>

        @GET("exchanges/tickers/{coinUid}")
        fun getMarketTickers(
            @Path("coinUid") coinUid: String,
            @Query("currency") currencyCode: String,
        ): Single<List<MarketTicker>>

        @GET("coins/top-movers")
        fun getTopMovers(
            @Query("currency") currencyCode: String
        ): Single<TopMoversRaw>

        @GET("status/updates")
        @Headers("Cache-Control: no-cache, no-store, must-revalidate")
        fun getStatus(): Single<HsStatus>

        @GET("coins/list")
        @Headers("Cache-Control: no-cache, no-store, must-revalidate")
        fun getAllCoins(): Single<List<CoinResponse>>

        @GET("blockchains/list")
        @Headers("Cache-Control: no-cache, no-store, must-revalidate")
        fun getAllBlockchains(): Single<List<BlockchainResponse>>

        @GET("tokens/list")
        @Headers("Cache-Control: no-cache, no-store, must-revalidate")
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

        @GET("exchanges/whitelist")
        fun verifiedExchangeUids(): Single<List<String>>

        @GET("exchanges/top-market-pairs")
        fun getTopPairs(
            @Query("currency") currencyCode: String,
            @Query("page") page: Int,
            @Query("limit") limit: Int
        ): Single<List<TopPair>>

        @POST("stats")
        @Headers("Content-Type: application/json")
        fun sendStats(
            @Header("app_platform") appPlatform: String,
            @Header("app_version") appVersion: String,
            @Header("app_id") appId: String?,
            @Body stats: String,
        ): Single<Unit>

        @GET("coins/signals")
        fun getCoinsSignals(
            @Query("uids") uids: String,
        ): Single<List<SignalResponse>>

        @GET("etfs")
        fun getEtfs(
            @Query("currency") currencyCode: String
        ): Single<List<EtfResponse>>

        @GET("etfs/total")
        fun getEtfPoints(
            @Query("currency") currencyCode: String
        ): Single<List<EtfPointResponse>>

        companion object {
            private const val marketInfoFields =
                "name,code,price,price_change_1d,price_change_24h,price_change_7d,price_change_30d,price_change_90d,market_cap_rank,coingecko_id,market_cap,market_cap_rank,total_volume"
            private const val topCoinsMarketInfoFields =
                "price,price_change_1d,price_change_24h,price_change_7d,price_change_30d,price_change_90d,market_cap_rank,market_cap,total_volume"
            private const val coinPriceFields =
                "price,price_change_1d,price_change_24h,last_updated"
            private const val advancedMarketFields =
                "all_platforms,price,market_cap,total_volume,price_change_1d,price_change_24h,price_change_7d,price_change_14d,price_change_30d,price_change_200d,price_change_1y,ath_percentage,atl_percentage"
        }
    }
}

data class HistoricalCoinPriceResponse(
    val timestamp: Long,
    val price: BigDecimal,
)

data class SignalResponse(
    val uid: String,
    val signal: Analytics.TechnicalAdvice.Advice?
)

data class ChartStart(val timestamp: Long)

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

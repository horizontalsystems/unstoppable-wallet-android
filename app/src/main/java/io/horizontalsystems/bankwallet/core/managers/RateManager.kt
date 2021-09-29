package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.CoinType
import io.reactivex.Observable
import io.reactivex.Single
import java.math.BigDecimal
import java.util.*

class RateManager(
    context: Context,
    private val appConfigProvider: IAppConfigProvider,
    private val marketKit: MarketKit
) : IRateManager
{
    override fun historicalRateCached(
        coinType: CoinType,
        currencyCode: String,
        timestamp: Long,
    ): BigDecimal? {
        return null
    }

    override fun historicalRate(
        coinType: CoinType,
        currencyCode: String,
        timestamp: Long,
    ): Single<BigDecimal> {
        return Single.error(NotImplementedError())
    }

    override fun chartInfo(
        coinType: CoinType,
        currencyCode: String,
        chartType: ChartType,
    ): ChartInfo? {
        return null
    }

    override fun chartInfoObservable(
        coinType: CoinType,
        currencyCode: String,
        chartType: ChartType,
    ): Observable<ChartInfo> {
        return Observable.error(NotImplementedError())
    }

    override fun coinMarketDetailsAsync(
        coinType: CoinType,
        currencyCode: String,
        rateDiffCoinCodes: List<String>,
        rateDiffPeriods: List<TimePeriod>,
    ): Single<CoinMarketDetails> {
        return Single.error(NotImplementedError())
    }

    override fun getTopTokenHoldersAsync(coinType: CoinType): Single<List<TokenHolder>> {
        return Single.error(NotImplementedError())
    }

    override fun getAuditsAsync(coinType: CoinType): Single<List<Auditor>> {
        return Single.error(NotImplementedError())
    }

    override fun getTopMarketList(
        currency: String,
        itemsCount: Int,
        diffPeriod: TimePeriod,
    ): Single<List<CoinMarket>> {
        return Single.error(NotImplementedError())
    }

    override fun getTopDefiTvlAsync(
        currencyCode: String,
        fetchDiffPeriod: TimePeriod,
        itemsCount: Int,
        chain: String?,
    ): Single<List<DefiTvl>> {
        return Single.error(NotImplementedError())
    }

    override fun getCoinMarketList(
        coinTypes: List<CoinType>,
        currency: String,
    ): Single<List<CoinMarket>> {
        return Single.error(NotImplementedError())
    }

    override fun getCoinMarketListByCategory(
        categoryId: String,
        currency: String,
    ): Single<List<CoinMarket>> {
        return Single.error(NotImplementedError())
    }

    override fun getCoinRatingsAsync(): Single<Map<CoinType, String>> {
        return Single.error(NotImplementedError())
    }

    override fun getGlobalMarketInfoAsync(currency: String): Single<GlobalCoinMarket> {
        return Single.error(NotImplementedError())
    }

    override fun getGlobalCoinMarketPointsAsync(
        currencyCode: String,
        timePeriod: TimePeriod,
    ): Single<List<GlobalCoinMarketPoint>> {
        return Single.error(NotImplementedError())
    }

    override fun searchCoins(searchText: String): List<CoinData> {
        return listOf()
    }

    override fun getNotificationCoinCode(coinType: CoinType): String? {
        return null
    }

    override fun topDefiTvl(
        currencyCode: String,
        fetchDiffPeriod: TimePeriod,
        itemsCount: Int,
    ): Single<List<DefiTvl>> {
        return Single.error(NotImplementedError())
    }

    override fun defiTvlPoints(
        coinType: CoinType,
        currencyCode: String,
        fetchDiffPeriod: TimePeriod,
    ): Single<List<DefiTvlPoint>> {
        return Single.error(NotImplementedError())
    }

    override fun getCoinMarketVolumePointsAsync(
        coinType: CoinType,
        currencyCode: String,
        fetchDiffPeriod: TimePeriod,
    ): Single<List<CoinMarketPoint>> {
        return Single.error(NotImplementedError())
    }

    override fun getCryptoNews(timestamp: Long?): Single<List<CryptoNews>> {
        return Single.error(NotImplementedError())
    }

    override fun refresh(currencyCode: String) = Unit

}

enum class ChartType(val interval: Long, val points: Int, val resource: String) {
    TODAY(30, 48, "histominute"),   // minutes
    DAILY(30, 48, "histominute"),   // minutes
    WEEKLY(4, 48, "histohour"),     // hourly
    WEEKLY2(8, 44, "histohour"),     // hourly
    MONTHLY(12, 60, "histohour"),   // hourly
    MONTHLY3(2, 45, "histoday"),    // daily
    MONTHLY6(3, 60, "histoday"),    // daily
    MONTHLY12(7, 52, "histoday"),   // daily
    MONTHLY24(14, 52, "histoday");  // daily

    val expirationInterval: Long
        get() {
            val multiplier = when (resource) {
                "histominute" -> 60
                "histohour" -> 60 * 60
                "histoday" -> 24 * 60 * 60
                else -> 60
            }

            return interval * multiplier
        }

    val rangeInterval: Long
        get() = expirationInterval * points

    val seconds: Long
        get() = when (this) {
            TODAY -> interval
            DAILY -> interval
            WEEKLY -> interval * 60
            WEEKLY2 -> interval * 60
            MONTHLY -> interval * 60
            MONTHLY3 -> interval * 24 * 60
            MONTHLY6 -> interval * 24 * 60
            MONTHLY12 -> interval * 24 * 60
            MONTHLY24 -> interval * 24 * 60
        } * 60

    val days: Int
        get() = when (this) {
            TODAY -> 1
            DAILY -> 1
            WEEKLY -> 7
            WEEKLY2 -> 14
            MONTHLY -> 30
            MONTHLY3 -> 90
            MONTHLY6 -> 180
            MONTHLY12 -> 360
            MONTHLY24 -> 720
        }

    companion object {
        private val map = values().associateBy(ChartType::name)

        fun fromString(type: String?): ChartType? = map[type]
    }
}

class ChartInfo(
    val points: List<ChartPoint>,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val isExpired: Boolean
)

data class ChartPoint(val value: BigDecimal, val volume: BigDecimal?, val timestamp: Long)

enum class TimePeriod(val id: Int, val seconds: Long, val title: String, val interval: Int = 0) {
    ALL(0, 0, "All", 0),
    HOUR_1(1, 3600, "1h", 0),
    DAY_START(2, 0, "DayStart", 30),
    HOUR_24(3, 86400, "24h", 30),
    DAY_7(4, 604800,"7d", 4),
    DAY_14(6, 604800,"14d", 8),
    DAY_30(5, 2592000, "30d", 12),
    DAY_200(5, 2592000, "200d", 3),
    YEAR_1(6, 31104000, "1y", 7);

    fun getChartType(): ChartType{
        return when(this){
            DAY_7 -> ChartType.WEEKLY
            DAY_14 -> ChartType.WEEKLY2
            DAY_30 -> ChartType.MONTHLY
            DAY_200 -> ChartType.MONTHLY6
            else -> ChartType.DAILY
        }
    }
}

class CoinMarketDetails(
    val data: CoinData,
    val meta: CoinMeta,
    val currencyCode: String,

    val rate: BigDecimal,
    val rateHigh24h: BigDecimal,
    val rateLow24h: BigDecimal,

    val totalSupply: BigDecimal,
    val circulatingSupply: BigDecimal,

    val volume24h: BigDecimal,

    val marketCap: BigDecimal,
    val marketCapDiff24h: BigDecimal?,
    val marketCapRank: Int?,

    val dilutedMarketCap: BigDecimal?,

    val rateDiffs: Map<TimePeriod, Map<String, BigDecimal>>,
    val tickers: List<MarketTicker>,

    var defiTvlInfo: DefiTvlInfo? = null
)

class DefiTvlInfo(
    val tvl: BigDecimal,
    val tvlRank: Int,
    val marketCapTvlRatio: BigDecimal
)

data class TokenHolder(
    val address: String,
    val share: BigDecimal
)

class CoinMarket(
    val data: CoinData,
    val marketInfo: MarketInfo
)

class MarketInfo(
    var coinType: CoinType,
    val currencyCode: String,
    val rate: BigDecimal,
    val rateOpenDay: BigDecimal,
    val rateDiff: BigDecimal,
    val volume: BigDecimal,
    val supply: BigDecimal,
    val rateDiffPeriod: BigDecimal?,
    val timestamp: Long,
    val marketCap: BigDecimal?,
    val athChangePercentage: BigDecimal?,
    val atlChangePercentage: BigDecimal?,
    val totalSupply: BigDecimal?,
    val dilutedMarketCap: BigDecimal?,
    val expirationInterval: Long){

    fun isExpired(): Boolean {
        return Date().time / 1000 - expirationInterval > timestamp
    }
}

class DefiTvl(
    val data: CoinData,
    val tvl: BigDecimal,
    val tvlDiff: BigDecimal,
    val tvlRank: Int = 0,
    val chains: List<String>?
)

data class GlobalCoinMarket(
    val currencyCode: String,
    val volume24h: BigDecimal,
    val volume24hDiff24h: BigDecimal,
    val marketCap: BigDecimal,
    val marketCapDiff24h: BigDecimal,
    val btcDominance: BigDecimal = BigDecimal.ZERO,
    val btcDominanceDiff24h: BigDecimal = BigDecimal.ZERO,
    val defiMarketCap: BigDecimal = BigDecimal.ZERO,
    val defiMarketCapDiff24h: BigDecimal = BigDecimal.ZERO,
    val defiTvl: BigDecimal = BigDecimal.ZERO,
    val defiTvlDiff24h: BigDecimal = BigDecimal.ZERO,
    val globalCoinMarketPoints: List<GlobalCoinMarketPoint>
){

    companion object {
        fun calculateData(currencyCode: String, globalMarketPoints: List<GlobalCoinMarketPoint>): GlobalCoinMarket {

            var marketCap = BigDecimal.ZERO
            var marketCapDiff = BigDecimal.ZERO
            var defiMarketCap = BigDecimal.ZERO
            var defiMarketCapDiff = BigDecimal.ZERO
            var volume24h = BigDecimal.ZERO
            var volume24hDiff = BigDecimal.ZERO
            var btcDominance = BigDecimal.ZERO
            var btcDominanceDiff = BigDecimal.ZERO
            var tvl = BigDecimal.ZERO
            var tvlDiff = BigDecimal.ZERO

            if(globalMarketPoints.isNotEmpty()){
                val startingPoint = globalMarketPoints.first()
                val endingPoint = globalMarketPoints.last()

                marketCap = endingPoint.marketCap
                marketCapDiff = calculateDiff(startingPoint.marketCap, marketCap)

                defiMarketCap = endingPoint.defiMarketCap
                defiMarketCapDiff = calculateDiff(startingPoint.defiMarketCap, defiMarketCap)

                volume24h = endingPoint.volume24h
                volume24hDiff = calculateDiff(startingPoint.volume24h, volume24h)

                btcDominance = endingPoint.btcDominance
                btcDominanceDiff = calculateDiff(startingPoint.btcDominance, btcDominance)

                tvl = endingPoint.defiTvl
                tvlDiff = calculateDiff(startingPoint.defiTvl, tvl)
            }

            return GlobalCoinMarket(currencyCode, volume24h, volume24hDiff, marketCap, marketCapDiff, btcDominance,
                btcDominanceDiff, defiMarketCap, defiMarketCapDiff, tvl, tvlDiff, globalMarketPoints)

        }

        private fun calculateDiff(sourceValue: BigDecimal, targetValue: BigDecimal): BigDecimal {
            return if(sourceValue.compareTo(BigDecimal.ZERO) != 0 )
                ((targetValue - sourceValue) * BigDecimal(100))/sourceValue
            else BigDecimal.ZERO
        }
    }
}

data class GlobalCoinMarketPoint(
    val id: Long = 0,
    val timestamp: Long,
    val volume24h: BigDecimal,
    val marketCap: BigDecimal,
    val btcDominance: BigDecimal = BigDecimal.ZERO,
    val defiMarketCap: BigDecimal = BigDecimal.ZERO,
    val defiTvl: BigDecimal = BigDecimal.ZERO,
    var pointInfoId: Long = 0
)

data class CoinData(
    var type: CoinType,
    var uid: String,
    val code: String,
    val title: String
)

class DefiTvlPoint(
    val timestamp: Long,
    val tvl: BigDecimal,
)


class CoinMarketPoint(
    val timestamp: Long,
    val marketCap: BigDecimal,
    val volume24h: BigDecimal,
)

data class CryptoNews(
    val id: Int,
    val source: String,
    val timestamp: Long,
    val imageUrl: String?,
    val title: String,
    val url: String,
    val body: String,
    val categories: List<String>
)

class CoinMeta(
    val description: String,
    val descriptionType: DescriptionType,
    val links: Map<LinkType, String>,
    val rating: String?,
    var categories: List<CoinCategory>,
    var fundCategories: List<CoinFundCategory>,
    val platforms: Map<CoinPlatformType, String>,
    val launchDate: Date? = null,
    val securityParameter: SecurityParameter? = null
) {
    enum class DescriptionType {
        HTML, MARKDOWN
    }
}

data class SecurityParameter(
    val coinType: CoinType,
    val privacy: Level,
    val decentralized: Boolean,
    val confiscationResistance: Boolean,
    val censorshipResistance: Boolean
)

enum class Level {
    LOW,
    MEDIUM,
    HIGH;

    companion object {
        fun intValue(level: Level): Int{
            return when(level){
                LOW -> 1
                MEDIUM -> 2
                HIGH -> 3
            }
        }

        fun fromInt(intValue: Int): Level {
            return when(intValue){
                1 -> LOW
                2 -> MEDIUM
                else -> HIGH
            }
        }
    }
}


data class CoinCategory(val id: String, val name: String)
enum class CoinPlatformType{
    OTHER,
    ETHEREUM,
    BINANCE,
    BINANCE_SMART_CHAIN,
    TRON,
    EOS,
}


data class CoinFundCategory(
    val id: String,
    val name: String,
    val order: Int){

    var funds = mutableListOf<CoinFund>()
}

data class CoinFund(
    val id: String,
    val name: String,
    val url: String,
    val categoryId: String)


data class Auditor(
    val id :String,
    val name :String,
) {
    val reports: MutableList<AuditReport> = mutableListOf()
}

data class AuditReport(
    val id :Long = 0,
    val name :String,
    val timestamp: Long,
    val issues: Int = 0,
    val link: String?
)

class MarketTicker(
    val base: String,
    val target: String,
    val marketName: String,
    val rate: BigDecimal,
    val volume: BigDecimal,
    val imageUrl: String? = null
)

enum class LinkType{
    GUIDE,
    WEBSITE,
    WHITEPAPER,
    TWITTER,
    TELEGRAM,
    REDDIT,
    GITHUB,
    YOUTUBE
}

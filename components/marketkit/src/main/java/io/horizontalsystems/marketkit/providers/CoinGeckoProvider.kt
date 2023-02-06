package io.horizontalsystems.marketkit.providers

import io.horizontalsystems.marketkit.models.CoinGeckoCoinResponse
import io.horizontalsystems.marketkit.models.Exchange
import io.horizontalsystems.marketkit.models.ExchangeRaw
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.math.BigDecimal

class CoinGeckoProvider(private val baseUrl: String) {

    private val coinGeckoService: CoinGeckoService by lazy {
        RetrofitUtils.build(baseUrl).create(CoinGeckoService::class.java)
    }

    fun exchangesSingle(limit: Int, page: Int): Single<List<Exchange>> {
        return coinGeckoService.exchanges(limit, page)
            .map { list ->
                list.map { Exchange(it.id, it.name, it.image) }
            }
    }

    fun marketTickersSingle(coinGeckoId: String): Single<CoinGeckoCoinResponse> {
        return coinGeckoService.marketTickers(
            coinGeckoId,
            "true",
            "false",
            "false",
            "false",
            "false",
            "false",
        )
    }

    interface CoinGeckoService {

        @GET("exchanges")
        fun exchanges(
            @Query("per_page") limit: Int,
            @Query("page") days: Int,
        ): Single<List<ExchangeRaw>>

        @GET("coins/{coinId}")
        fun marketTickers(
            @Path("coinId") coinId: String,
            @Query("tickers") tickers: String,
            @Query("localization") localization: String,
            @Query("market_data") marketData: String,
            @Query("community_data") communityData: String,
            @Query("developer_data") developerData: String,
            @Query("sparkline") sparkline: String,
        ): Single<CoinGeckoCoinResponse>


        object Response {
            data class HistoricalMarketData(
                val prices: List<List<BigDecimal>>,
                val market_caps: List<List<BigDecimal>>,
                val total_volumes: List<List<BigDecimal>>,
            )
        }
    }

}

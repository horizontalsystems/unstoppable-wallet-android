package io.horizontalsystems.marketkit.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.util.*

data class MarketInfoOverviewRaw(
    val performance: Map<String, Map<String, BigDecimal?>>,
    @SerializedName("genesis_date")
    val genesisDate: Date?,
    val categories: List<CoinCategory>,
    val description: String?,
    val links: Map<String, String>,
    @SerializedName("market_data")
    val marketData: MarketData,
) {

    fun marketInfoOverview(fullCoin: FullCoin): MarketInfoOverview {
            val performance = performance.map { (vsCurrency, v) ->
                vsCurrency to v.mapNotNull { (timePeriodRaw, performance) ->
                    if (performance == null) return@mapNotNull null

                    val timePeriod = when (timePeriodRaw) {
                        "7d" -> HsTimePeriod.Week1
                        "30d" -> HsTimePeriod.Month1
                        else -> return@mapNotNull null
                    }

                    timePeriod to performance
                }.toMap()
            }.toMap()

            val links = links
                .mapNotNull { (linkTypeRaw, link) ->
                    LinkType.fromString(linkTypeRaw)?.let {
                        it to link
                    }
                }.toMap()

            return MarketInfoOverview(
                fullCoin,
                marketData.marketCap,
                marketData.marketCapRank,
                marketData.totalSupply,
                marketData.circulatingSupply,
                marketData.volume24h,
                marketData.dilutedMarketCap,
                marketData.tvl,
                performance,
                genesisDate,
                categories,
                description ?: "",
                links,
            )
        }

    data class MarketData(
        @SerializedName("market_cap")
        val marketCap: BigDecimal?,
        @SerializedName("market_cap_rank")
        val marketCapRank: Int?,
        @SerializedName("total_supply")
        val totalSupply: BigDecimal?,
        @SerializedName("circulating_supply")
        val circulatingSupply: BigDecimal?,
        @SerializedName("total_volume")
        val volume24h: BigDecimal?,
        @SerializedName("fully_diluted_valuation")
        val dilutedMarketCap: BigDecimal?,
        val tvl: BigDecimal?,
    )
}

package io.horizontalsystems.marketkit.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class MarketGlobal(
    @SerializedName("market_cap")
    val marketCap: BigDecimal?,
    @SerializedName("market_cap_change")
    val marketCapChange: BigDecimal?,
    @SerializedName("defi_market_cap")
    val defiMarketCap: BigDecimal?,
    @SerializedName("defi_market_cap_change")
    val defiMarketCapChange: BigDecimal?,
    val volume: BigDecimal?,
    @SerializedName("volume_change")
    val volumeChange: BigDecimal?,
    @SerializedName("btc_dominance")
    val btcDominance: BigDecimal?,
    @SerializedName("btc_dominance_change")
    val btcDominanceChange: BigDecimal?,
    val tvl: BigDecimal?,
    @SerializedName("tvl_change")
    val tvlChange: BigDecimal?,
    @SerializedName("etf_total_inflow")
    val etfTotalInflow: BigDecimal?,
    @SerializedName("etf_daily_inflow")
    val etfDailyInflow: BigDecimal?
)
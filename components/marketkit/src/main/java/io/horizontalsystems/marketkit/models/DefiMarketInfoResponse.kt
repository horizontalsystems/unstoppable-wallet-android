package io.horizontalsystems.marketkit.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class DefiMarketInfoResponse(
    val uid: String?,
    val name: String,
    @SerializedName("logo")
    val logoUrl: String,
    val tvl: BigDecimal,
    @SerializedName("tvl_rank")
    val tvlRank: Int,
    @SerializedName("tvl_change_1d")
    val tvlChange1D: BigDecimal?,
    @SerializedName("tvl_change_1w")
    val tvlChange1W: BigDecimal?,
    @SerializedName("tvl_change_2w")
    val tvlChange2W: BigDecimal?,
    @SerializedName("tvl_change_1m")
    val tvlChange1M: BigDecimal?,
    @SerializedName("tvl_change_3m")
    val tvlChange3M: BigDecimal?,
    @SerializedName("tvl_change_6m")
    val tvlChange6M: BigDecimal?,
    @SerializedName("tvl_change_1y")
    val tvlChange1Y: BigDecimal?,
    val chains: List<String>,
    @SerializedName("chain_tvls")
    val chainTvls: Map<String, BigDecimal?>,
)

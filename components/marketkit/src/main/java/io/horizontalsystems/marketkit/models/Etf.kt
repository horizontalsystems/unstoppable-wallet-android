package io.horizontalsystems.marketkit.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

data class Etf(
    val ticker: String,
    val name: String,
    val date: Date?,
    val totalAssets: BigDecimal?,
    val totalInflow: BigDecimal?,
    val inflows: Map<HsTimePeriod, BigDecimal?>
)

data class EtfResponse(
    val ticker: String,
    val name: String,
    val date: String?,
    @SerializedName("total_assets")
    val totalAssets: BigDecimal?,
    @SerializedName("total_inflow")
    val totalInflow: BigDecimal?,
    @SerializedName("inflow_1d")
    val inflow1d: BigDecimal?,
    @SerializedName("inflow_1w")
    val inflow1w: BigDecimal?,
    @SerializedName("inflow_1m")
    val inflow1m: BigDecimal?,
    @SerializedName("inflow_3m")
    val inflow3m: BigDecimal?,
) {
    companion object {
        fun toEtf(response: EtfResponse): Etf {
            val dateFormatter by lazy {
                val formatter = SimpleDateFormat("yyyy-MM-dd")
                formatter.timeZone = TimeZone.getTimeZone("GMT")
                formatter
            }

            return Etf(
                response.ticker,
                response.name,
                response.date?.let { dateFormatter.parse(it) },
                response.totalAssets,
                response.totalInflow,
                mapOf(
                    HsTimePeriod.Day1 to response.inflow1d,
                    HsTimePeriod.Week1 to response.inflow1w,
                    HsTimePeriod.Month1 to response.inflow1m,
                    HsTimePeriod.Month3 to response.inflow3m
                )
            )
        }
    }
}
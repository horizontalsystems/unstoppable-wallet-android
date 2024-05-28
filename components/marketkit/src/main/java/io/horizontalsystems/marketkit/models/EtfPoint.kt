package io.horizontalsystems.marketkit.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

data class EtfPoint(
    val date: Date,
    val totalAssets: BigDecimal,
    val totalInflow: BigDecimal,
    val dailyInflow: BigDecimal
)

data class EtfPointResponse(
    val date: String,
    @SerializedName("total_assets")
    val totalAssets: BigDecimal,
    @SerializedName("total_inflow")
    val totalInflow: BigDecimal,
    @SerializedName("daily_inflow")
    val dailyInflow: BigDecimal
) {
    companion object {
        fun toEtfPoint(response: EtfPointResponse): EtfPoint? {
            val dateFormatter by lazy {
                val formatter = SimpleDateFormat("yyyy-MM-dd")
                formatter.timeZone = TimeZone.getTimeZone("GMT")
                formatter
            }
            val date = dateFormatter.parse(response.date) ?: return null
            return EtfPoint(
                date,
                response.totalAssets,
                response.totalInflow,
                response.dailyInflow
            )
        }
    }
}
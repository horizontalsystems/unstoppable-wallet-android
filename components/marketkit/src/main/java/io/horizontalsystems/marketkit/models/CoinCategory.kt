package io.horizontalsystems.marketkit.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class CoinCategory(
    val uid: String,
    val name: String,
    val description: Map<String, String>,
    @SerializedName("market_cap")
    val marketCap: BigDecimal?,
    @SerializedName("change_24h")
    val diff24H: BigDecimal?,
    @SerializedName("change_1w")
    val diff1W: BigDecimal?,
    @SerializedName("change_1m")
    val diff1M: BigDecimal?,
) : Parcelable {

    override fun toString(): String {
        return "CoinCategory [uid: $uid; name: $name; descriptionCount: ${description.size}]"
    }

}

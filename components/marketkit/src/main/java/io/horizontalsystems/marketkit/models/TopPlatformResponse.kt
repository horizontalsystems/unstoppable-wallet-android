package io.horizontalsystems.marketkit.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class TopPlatformResponse(
    val uid: String,
    val name: String,
    val rank: Int,
    val protocols: Int,
    @SerializedName("market_cap")
    val marketCap: BigDecimal,
    val stats: Map<String, BigDecimal?>,
) {

    val topPlatform: TopPlatform
        get() =
            TopPlatform(
                Blockchain(BlockchainType.fromUid(uid), name, null),
                rank,
                protocols,
                marketCap,
                stats["rank_1d"]?.toInt(),
                stats["rank_1w"]?.toInt(),
                stats["rank_1m"]?.toInt(),
                stats["change_1d"],
                stats["change_1w"],
                stats["change_1m"]
            )

}

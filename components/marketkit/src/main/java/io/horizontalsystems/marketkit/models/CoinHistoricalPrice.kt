package io.horizontalsystems.marketkit.models

import androidx.room.Entity
import java.math.BigDecimal

@Entity(primaryKeys = ["coinUid", "currencyCode", "timestamp"])
class CoinHistoricalPrice(
    val coinUid: String,
    val currencyCode: String,
    val value: BigDecimal,
    val timestamp: Long
)

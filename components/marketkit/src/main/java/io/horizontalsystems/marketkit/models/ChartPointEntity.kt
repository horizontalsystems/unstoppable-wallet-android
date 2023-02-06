package io.horizontalsystems.marketkit.models

import androidx.room.Entity
import java.math.BigDecimal

@Entity(primaryKeys = ["coinUid", "currencyCode", "interval", "timestamp"])
data class ChartPointEntity(
    val coinUid: String,
    val currencyCode: String,
    val interval: HsPeriodType,
    val value: BigDecimal,
    val volume: BigDecimal?,
    val timestamp: Long
)

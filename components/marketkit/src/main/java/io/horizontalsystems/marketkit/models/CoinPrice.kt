package io.horizontalsystems.marketkit.models

import androidx.room.Entity
import java.math.BigDecimal
import java.util.*

@Entity(primaryKeys = ["coinUid", "currencyCode"])
data class CoinPrice(
    val coinUid: String,
    val currencyCode: String,
    val value: BigDecimal,
    val diff: BigDecimal?,
    val timestamp: Long
) {
    val expired: Boolean
        get() = Date().time / 1000 - expirationInterval > timestamp

    override fun toString(): String {
        return "CoinPrice [coinUid: $coinUid; currencyCode: $currencyCode; value: $value; diff: $diff; timestamp: $timestamp]"
    }

    companion object {
        const val expirationInterval: Long = 240
    }
}

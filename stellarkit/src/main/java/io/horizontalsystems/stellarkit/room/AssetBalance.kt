package io.horizontalsystems.stellarkit.room

import androidx.room.Entity
import java.math.BigDecimal

@Entity(primaryKeys = ["code", "issuer"])
data class AssetBalance(
    val type: String,
    val code: String,
    val issuer: String,
    val balance: BigDecimal,
)

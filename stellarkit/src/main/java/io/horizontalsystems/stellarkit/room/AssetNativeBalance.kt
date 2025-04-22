package io.horizontalsystems.stellarkit.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity
data class AssetNativeBalance(
    @PrimaryKey
    val key: String = "native",
    val balance: BigDecimal
)
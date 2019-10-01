package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity
data class PriceAlertRecord(@PrimaryKey val coinCode: String, val stateRaw: Int, val lastRate: BigDecimal?)

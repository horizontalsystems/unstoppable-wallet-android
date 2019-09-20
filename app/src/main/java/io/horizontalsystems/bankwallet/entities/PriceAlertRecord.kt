package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PriceAlertRecord(@PrimaryKey val coinCode: String, val stateRaw: Int)

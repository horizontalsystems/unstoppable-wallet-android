package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class EvmMethodLabel(
    @PrimaryKey
    val methodId: String,
    val label: String
)

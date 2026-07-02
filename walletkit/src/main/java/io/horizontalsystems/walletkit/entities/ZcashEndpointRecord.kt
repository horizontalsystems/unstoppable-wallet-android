package io.horizontalsystems.walletkit.entities

import androidx.room.Entity

@Entity(primaryKeys = ["url"])
data class ZcashEndpointRecord(
    val url: String,
)

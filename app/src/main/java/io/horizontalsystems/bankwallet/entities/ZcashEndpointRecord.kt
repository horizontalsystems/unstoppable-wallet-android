package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity

@Entity(primaryKeys = ["url"])
data class ZcashEndpointRecord(
    val url: String,
)

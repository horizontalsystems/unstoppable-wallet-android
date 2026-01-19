package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity

@Entity(primaryKeys = ["providerId", "tokenQueryId"])
data class SwapProviderAssetRecord(
    val providerId: String,
    val tokenQueryId: String,
    val data: String,
    val timestamp: Long
)
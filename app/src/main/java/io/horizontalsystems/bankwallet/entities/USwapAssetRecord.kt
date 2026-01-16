package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity

@Entity(primaryKeys = ["providerId", "tokenQueryId"])
data class USwapAssetRecord(
    val providerId: String,
    val tokenQueryId: String,
    val assetIdentifier: String,
    val timestamp: Long
)

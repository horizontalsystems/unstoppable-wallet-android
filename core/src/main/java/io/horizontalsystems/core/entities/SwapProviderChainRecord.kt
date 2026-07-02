package io.horizontalsystems.core.entities

import androidx.room.Entity

@Entity(primaryKeys = ["providerId", "chainId"])
data class SwapProviderChainRecord(
    val providerId: String,
    val chainId: String,
    val timestamp: Long,
)

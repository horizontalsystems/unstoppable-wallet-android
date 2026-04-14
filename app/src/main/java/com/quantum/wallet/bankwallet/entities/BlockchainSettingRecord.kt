package com.quantum.wallet.bankwallet.entities

import androidx.room.Entity

@Entity(primaryKeys = ["blockchainUid", "key"])
data class BlockchainSettingRecord(
    val blockchainUid: String,
    val key: String,
    val value: String
)

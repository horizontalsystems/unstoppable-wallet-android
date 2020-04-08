package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity

@Entity(primaryKeys = ["coinType", "key"])
data class BlockchainSetting(
        val coinType: CoinType,
        val key: String,
        val value: String)
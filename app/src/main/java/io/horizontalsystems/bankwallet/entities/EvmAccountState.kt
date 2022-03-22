package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity

@Entity(primaryKeys = ["accountId", "chainId"])
data class EvmAccountState(
    val accountId: String,
    val chainId: Int,
    val transactionsSyncedBlockNumber: Long
)

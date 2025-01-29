package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import io.horizontalsystems.marketkit.models.BlockchainType

@Entity(primaryKeys = ["accountId", "blockchainType"])
data class RecentAddress(
    val accountId: String,
    val blockchainType: BlockchainType,
    val address: String
)

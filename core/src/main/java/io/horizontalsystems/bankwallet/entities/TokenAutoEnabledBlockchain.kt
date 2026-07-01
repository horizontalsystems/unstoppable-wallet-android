package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import io.horizontalsystems.marketkit.models.BlockchainType

@Entity(primaryKeys = ["accountId", "blockchainType"])
data class TokenAutoEnabledBlockchain(
    val accountId: String,
    val blockchainType: BlockchainType
)

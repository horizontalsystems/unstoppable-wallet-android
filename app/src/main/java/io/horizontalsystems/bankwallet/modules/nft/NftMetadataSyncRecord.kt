package io.horizontalsystems.bankwallet.modules.nft

import androidx.room.Entity
import io.horizontalsystems.marketkit.models.BlockchainType

@Entity(primaryKeys = ["blockchainType", "accountId"])
data class NftMetadataSyncRecord(
    val blockchainType: BlockchainType,
    val accountId: String,
    val lastSyncTimestamp: Long
)

package cash.p.terminal.entities.nft

import androidx.room.Entity
import io.horizontalsystems.core.entities.BlockchainType

@Entity(primaryKeys = ["blockchainType", "accountId"])
data class NftMetadataSyncRecord(
    val blockchainType: BlockchainType,
    val accountId: String,
    val lastSyncTimestamp: Long
)

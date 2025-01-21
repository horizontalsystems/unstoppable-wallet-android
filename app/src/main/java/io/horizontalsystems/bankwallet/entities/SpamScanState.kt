package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import io.horizontalsystems.marketkit.models.BlockchainType

@Entity(primaryKeys = ["blockchainType", "accountId"])
class SpamScanState(
    val blockchainType: BlockchainType,
    val accountId: String,
    val lastTransactionHash: ByteArray
)
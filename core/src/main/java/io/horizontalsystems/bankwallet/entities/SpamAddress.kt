package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import io.horizontalsystems.marketkit.models.BlockchainType

@Entity(primaryKeys = ["transactionHash", "address"])
class SpamAddress(
    val transactionHash: ByteArray,
    val address: String,
    val domain: String?,
    val blockchainType: BlockchainType?
)
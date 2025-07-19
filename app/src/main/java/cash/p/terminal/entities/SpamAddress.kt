package cash.p.terminal.entities

import androidx.room.Entity
import io.horizontalsystems.core.entities.BlockchainType

@Entity(primaryKeys = ["transactionHash", "address"])
class SpamAddress(
    val transactionHash: ByteArray,
    val address: String,
    val domain: String?,
    val blockchainType: BlockchainType?
)
package cash.p.terminal.entities

import androidx.room.Entity
import io.horizontalsystems.core.entities.BlockchainType

@Entity(primaryKeys = ["blockchainType", "accountId"])
class SpamScanState(
    val blockchainType: BlockchainType,
    val accountId: String,
    val lastTransactionHash: ByteArray
)
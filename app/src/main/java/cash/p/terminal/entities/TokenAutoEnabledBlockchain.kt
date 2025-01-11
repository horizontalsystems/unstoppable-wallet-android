package cash.p.terminal.entities

import androidx.room.Entity
import io.horizontalsystems.core.entities.BlockchainType

@Entity(primaryKeys = ["accountId", "blockchainType"])
data class TokenAutoEnabledBlockchain(
    val accountId: String,
    val blockchainType: BlockchainType
)

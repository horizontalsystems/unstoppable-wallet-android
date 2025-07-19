package cash.p.terminal.entities

import androidx.room.Entity
import io.horizontalsystems.core.entities.BlockchainType

@Entity(primaryKeys = ["accountId", "blockchainType"])
data class RecentAddress(
    val accountId: String,
    val blockchainType: BlockchainType,
    val address: String
)

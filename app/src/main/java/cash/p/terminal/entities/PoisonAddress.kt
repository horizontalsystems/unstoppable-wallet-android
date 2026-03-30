package cash.p.terminal.entities

import androidx.room.Entity

enum class PoisonAddressType {
    KNOWN,
    SCAM,
}

@Entity(primaryKeys = ["address", "blockchainTypeUid"])
data class PoisonAddress(
    val address: String,
    val blockchainTypeUid: String,
    val type: PoisonAddressType,
)

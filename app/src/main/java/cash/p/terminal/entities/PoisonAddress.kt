package cash.p.terminal.entities

import androidx.room.Entity

enum class PoisonAddressType {
    KNOWN,
    SCAM,
}

@Entity(primaryKeys = ["address", "blockchainTypeUid", "accountId"])
data class PoisonAddress(
    val address: String,
    val blockchainTypeUid: String,
    val accountId: String,
    val type: PoisonAddressType,
)

package cash.p.terminal.entities

import androidx.room.Entity

@Entity(primaryKeys = ["blockchainUid", "key"])
data class BlockchainSettingRecord(
    val blockchainUid: String,
    val key: String,
    val value: String
)

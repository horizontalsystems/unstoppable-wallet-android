package cash.p.terminal.entities

import androidx.room.Entity

@Entity(primaryKeys = ["accountId", "blockchainTypeUid", "key"])
class RestoreSettingRecord(
        val accountId: String,
        val blockchainTypeUid: String,
        val key: String,
        val value: String,
)

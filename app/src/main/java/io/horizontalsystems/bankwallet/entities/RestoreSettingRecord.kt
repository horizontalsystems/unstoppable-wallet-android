package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity

@Entity(primaryKeys = ["accountId", "coinId", "key"])
class RestoreSettingRecord(
        val accountId: String,
        val coinId: String,
        val key: String,
        val value: String,
)

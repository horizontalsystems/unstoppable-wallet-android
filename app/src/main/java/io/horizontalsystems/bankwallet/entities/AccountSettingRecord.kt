package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity

@Entity(primaryKeys = ["accountId", "key"])
data class AccountSettingRecord(val accountId: String, val key: String, val value: String)

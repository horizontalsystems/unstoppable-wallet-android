package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity

@Entity(primaryKeys = ["primaryKey"])
class ActiveAccount(
        val accountId: String,
        val primaryKey: String = "active_account"
)

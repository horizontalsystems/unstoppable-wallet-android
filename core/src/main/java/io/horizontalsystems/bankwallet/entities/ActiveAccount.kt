package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class ActiveAccount(
    @PrimaryKey
    val level: Int,
    val accountId: String,
)

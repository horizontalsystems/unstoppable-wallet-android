package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import io.horizontalsystems.bankwallet.core.storage.AccountRecord

@Entity(primaryKeys = ["coinId", "accountId"],
        foreignKeys = [ForeignKey(
                entity = AccountRecord::class,
                parentColumns = ["id"],
                childColumns = ["accountId"],
                onUpdate = ForeignKey.CASCADE,
                onDelete = ForeignKey.CASCADE,
                deferred = true)
        ],
        indices = [Index("accountId", name = "index_EnabledWallet_accountId")])

data class EnabledWallet(
        val coinId: String,
        val accountId: String,
        val walletOrder: Int? = null
)

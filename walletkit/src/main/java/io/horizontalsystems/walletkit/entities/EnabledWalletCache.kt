package io.horizontalsystems.walletkit.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import io.horizontalsystems.walletkit.core.BalanceData
import io.horizontalsystems.walletkit.core.storage.AccountRecord

@Entity(
    primaryKeys = ["tokenQueryId", "accountId"],
    foreignKeys = [ForeignKey(
        entity = AccountRecord::class,
        parentColumns = ["id"],
        childColumns = ["accountId"],
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.CASCADE,
        deferred = true
    )]
)
data class EnabledWalletCache(
    val tokenQueryId: String,
    val accountId: String,
    val balanceData: BalanceData?,
)

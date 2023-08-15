package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import io.horizontalsystems.bankwallet.core.storage.AccountRecord
import java.math.BigDecimal

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
    val balance: BigDecimal,
    val balanceLocked: BigDecimal,
)

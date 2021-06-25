package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import io.horizontalsystems.bankwallet.core.storage.AccountRecord
import java.math.BigDecimal

@Entity(
    primaryKeys = ["coinId", "coinSettingsId", "accountId"],
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
    val coinId: String,
    val coinSettingsId: String,
    val accountId: String,
    val balance: BigDecimal,
    val balanceLocked: BigDecimal,
)

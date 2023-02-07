package cash.p.terminal.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import cash.p.terminal.core.storage.AccountRecord
import java.math.BigDecimal

@Entity(
    primaryKeys = ["tokenQueryId", "coinSettingsId", "accountId"],
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
    val coinSettingsId: String,
    val accountId: String,
    val balance: BigDecimal,
    val balanceLocked: BigDecimal,
)

package cash.p.terminal.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import cash.p.terminal.wallet.entities.AccountRecord
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

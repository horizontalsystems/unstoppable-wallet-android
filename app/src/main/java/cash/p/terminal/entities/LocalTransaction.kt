package cash.p.terminal.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import cash.p.terminal.wallet.entities.TokenQuery
import java.math.BigDecimal

@Entity(primaryKeys = ["transactionId", "tokenQuery"])
data class LocalTransaction(
    @PrimaryKey
    val date: Long = System.currentTimeMillis(),
    val transactionId: String,
    val address: String,
    val amount: BigDecimal,
    val tokenQuery: TokenQuery,
    val accountId: String
)

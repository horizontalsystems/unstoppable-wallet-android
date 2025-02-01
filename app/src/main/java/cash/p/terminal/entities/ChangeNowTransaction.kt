package cash.p.terminal.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import cash.p.terminal.network.changenow.domain.entity.TransactionStatusEnum
import java.math.BigDecimal

@Entity
data class ChangeNowTransaction(
    @PrimaryKey
    val date: Long = System.currentTimeMillis(),
    val transactionId: String,
    val status: String,

    val coinUidIn: String,
    val blockchainTypeIn: String,
    val amountIn: BigDecimal,
    val addressIn: String,

    val coinUidOut: String,
    val blockchainTypeOut: String,
    val amountOut: BigDecimal,
    val addressOut: String
) {
    fun isFinished() = status == TransactionStatusEnum.FINISHED.name.lowercase() ||
            status == TransactionStatusEnum.FAILED.name.lowercase() ||
            status == TransactionStatusEnum.REFUNDED.name.lowercase()
}

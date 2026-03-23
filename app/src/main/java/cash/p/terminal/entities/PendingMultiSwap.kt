package cash.p.terminal.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import cash.p.terminal.network.swaprepository.SwapProvider
import java.math.BigDecimal

@Entity
data class PendingMultiSwap(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val coinUidIn: String,
    val blockchainTypeIn: String,
    val amountIn: BigDecimal,
    val coinUidIntermediate: String,
    val blockchainTypeIntermediate: String,
    val coinUidOut: String,
    val blockchainTypeOut: String,
    val leg1ProviderId: String,
    val leg1IsOffChain: Boolean,
    val leg1TransactionId: String?,
    val leg1AmountOut: BigDecimal?,
    val leg1Status: String,
    val leg2ProviderId: String?,
    val leg2IsOffChain: Boolean?,
    val leg2TransactionId: String?,
    val leg2AmountOut: BigDecimal?,
    val leg2Status: String,
    val expectedAmountOut: BigDecimal,
    val leg2StartedAt: Long? = null,
    val leg1ProviderTransactionId: String? = null,
    val leg2ProviderTransactionId: String? = null,
    val leg1InfoRecordUid: String? = null,
) {
    fun isTerminal(): Boolean =
        leg1Status == STATUS_FAILED ||
        leg2Status in TERMINAL_STATUSES

    fun leg2StartTime(): Long = leg2StartedAt ?: createdAt

    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_EXECUTING = "executing"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_FAILED = "failed"

        private val TERMINAL_STATUSES = listOf(STATUS_COMPLETED, STATUS_FAILED)

        fun mapProviderIdToSwapProvider(providerId: String): SwapProvider? = when (providerId) {
            "changenow" -> SwapProvider.CHANGENOW
            "quickex" -> SwapProvider.QUICKEX
            else -> null
        }
    }
}

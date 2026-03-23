package cash.p.terminal.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import cash.p.terminal.network.changenow.api.ChangeNowHelper
import cash.p.terminal.network.changenow.domain.entity.TransactionStatusEnum
import cash.p.terminal.network.quickex.api.QuickexHelper
import cash.p.terminal.network.swaprepository.SwapProvider
import java.math.BigDecimal

@Entity
data class SwapProviderTransaction(
    @PrimaryKey
    val date: Long = System.currentTimeMillis(),
    val outgoingRecordUid: String?,
    val transactionId: String,
    val status: String,
    val provider: SwapProvider,

    val coinUidIn: String,
    val blockchainTypeIn: String,
    val amountIn: BigDecimal,
    val addressIn: String,

    val coinUidOut: String,
    val blockchainTypeOut: String,
    val amountOut: BigDecimal,
    val addressOut: String,

    val amountOutReal: BigDecimal? = null,
    val finishedAt: Long? = null,
    val incomingRecordUid: String? = null
) {
    fun isFinished() = status in FINISHED_STATUSES

    fun toStatusUrl(): Pair<String, String>? = when (provider) {
        SwapProvider.CHANGENOW -> ChangeNowHelper.CHANGE_NOW_URL to ChangeNowHelper.getViewTransactionUrl(transactionId)
        SwapProvider.QUICKEX -> QuickexHelper.QUICKEX_URL to QuickexHelper.getViewTransactionUrl(transactionId, addressOut)
    }

    companion object {
        val FINISHED_STATUSES = listOf(
            TransactionStatusEnum.FINISHED.name.lowercase(),
            TransactionStatusEnum.FAILED.name.lowercase(),
            TransactionStatusEnum.REFUNDED.name.lowercase()
        )
    }
}

package io.horizontalsystems.walletkit.entities.transactionrecords.tron

import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.walletkit.modules.transactions.TransactionStatus
import io.horizontalsystems.marketkit.models.Token

/** Kit-free projection of tron-kit's Transaction — the fields records and UI consume. */
data class TronTransactionInfo(
    val hashString: String,
    val blockNumber: Long?,
    val timestamp: Long,
    val isFailed: Boolean,
    val confirmed: Boolean,
    val fee: Long?,
    val contractLabel: String?,
)

open class TronTransactionRecord(
    val transaction: TronTransactionInfo,
    baseToken: Token,
    source: TransactionSource,
    val foreignTransaction: Boolean = false,
    spam: Boolean = false
) :
    TransactionRecord(
        uid = transaction.hashString,
        transactionHash = transaction.hashString,
        transactionIndex = 0,
        blockHeight = transaction.blockNumber?.toInt(),
        confirmationsThreshold = CONFIRMATIONS_THRESHOLD,
        timestamp = transaction.timestamp / 1000,
        failed = transaction.isFailed,
        spam = spam,
        source = source
    ) {

    val fee: TransactionValue?

    init {
        val feeAmount: Long? = transaction.fee
        fee = if (feeAmount != null) {
            val feeDecimal = feeAmount.toBigDecimal()
                .movePointLeft(baseToken.decimals).stripTrailingZeros()

            TransactionValue.CoinValue(baseToken, feeDecimal)
        } else {
            null
        }
    }

    override fun status(lastBlockHeight: Int?): TransactionStatus {
        when {
            failed -> {
                return TransactionStatus.Failed
            }

            transaction.confirmed -> {
                return TransactionStatus.Completed
            }

            blockHeight != null && lastBlockHeight != null -> {
                val threshold = confirmationsThreshold ?: 1
                val confirmations = lastBlockHeight - blockHeight.toInt() + 1

                return if (confirmations >= threshold) {
                    TransactionStatus.Completed
                } else {
                    TransactionStatus.Processing(confirmations.toFloat() / threshold.toFloat())
                }
            }

            else -> return TransactionStatus.Pending
        }
    }

    companion object {
        const val CONFIRMATIONS_THRESHOLD: Int = 19
    }

}

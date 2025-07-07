package cash.p.terminal.entities.transactionrecords.monero

import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.modules.transactions.TransactionStatus
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.transaction.TransactionSource
import java.math.BigDecimal

class MoneroTransactionRecord(
    uid: String,
    transactionHash: String,
    blockHeight: Int?,
    confirmationsThreshold: Int?,
    timestamp: Long,
    failed: Boolean = false,
    source: TransactionSource,
    transactionRecordType: TransactionRecordType,
    token: Token,
    to: String? = null,
    from: String? = null,
    sentToSelf: Boolean = false,
    memo: String? = null,
    amount: BigDecimal,
    val fee: TransactionValue,
    val subaddressLabel: String?,
    val isPending: Boolean,
    val confirmations: Long,
    override val mainValue: TransactionValue = TransactionValue.CoinValue(token, amount),
) : TransactionRecord(
    uid = uid,
    transactionHash = transactionHash,
    transactionIndex = 0,
    blockHeight = blockHeight,
    confirmationsThreshold = confirmationsThreshold,
    timestamp = timestamp,
    failed = failed,
    spam = false,
    source = source,
    transactionRecordType = transactionRecordType,
    token = token,
    to = to,
    from = from,
    sentToSelf = sentToSelf,
    memo = memo,
) {
    override fun status(lastBlockHeight: Int?): TransactionStatus {
        if (failed) {
            return TransactionStatus.Failed
        } else if (isPending) {
            return TransactionStatus.Pending
        } else if (blockHeight != null && lastBlockHeight != null) {
            val threshold = confirmationsThreshold ?: 1

            return if (confirmations >= threshold) {
                TransactionStatus.Completed
            } else {
                TransactionStatus.Processing(confirmations.toFloat() / threshold.toFloat())
            }
        }

        return TransactionStatus.Pending
    }
}

package cash.p.terminal.entities.transactionrecords.solana

import cash.p.terminal.core.adapters.BaseSolanaAdapter
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.solanakit.models.Transaction

class SolanaTransactionRecord(
    sentToSelf: Boolean = false,
    to: String? = null,
    from: String? = null,
    token: Token,
    source: TransactionSource,
    transactionRecordType: TransactionRecordType,
    spam: Boolean = false,
    transaction: Transaction,
    val incomingSolanaTransfers: List<SolanaTransfer>? = null,
    val outgoingSolanaTransfers: List<SolanaTransfer>? = null,
    override val mainValue: TransactionValue? = null,
) : TransactionRecord(
    uid = transaction.hash,
    transactionHash = transaction.hash,
    transactionIndex = 0,
    blockHeight = if (transaction.pending) null else 0,
    confirmationsThreshold = BaseSolanaAdapter.confirmationsThreshold,
    timestamp = transaction.timestamp,
    failed = transaction.error != null,
    spam = spam,
    source = source,
    transactionRecordType = transactionRecordType,
    token = token,
    to = to,
    from = from,
    sentToSelf = sentToSelf,
) {

    data class SolanaTransfer(val address: String?, val value: TransactionValue)

    val fee: TransactionValue? = transaction.fee?.let { TransactionValue.CoinValue(token, it) }
}

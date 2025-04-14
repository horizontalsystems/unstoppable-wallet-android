package cash.p.terminal.entities.transactionrecords.solana

import cash.p.terminal.core.adapters.BaseSolanaAdapter
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.solanakit.models.Transaction

class SolanaTransactionRecord(
    val sentToSelf: Boolean = false,
    val incomingSolanaTransfers: List<SolanaTransfer>? = null,
    val outgoingSolanaTransfers: List<SolanaTransfer>? = null,
    transaction: Transaction,
    override val mainValue: TransactionValue? = null,
    val to: String? = null,
    val from: String? = null,
    val baseToken: Token,
    source: TransactionSource,
    transactionRecordType: TransactionRecordType,
    spam: Boolean = false,
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
    transactionRecordType = transactionRecordType
) {

    data class SolanaTransfer(val address: String?, val value: TransactionValue)

    val fee: TransactionValue? = transaction.fee?.let { TransactionValue.CoinValue(baseToken, it) }
}

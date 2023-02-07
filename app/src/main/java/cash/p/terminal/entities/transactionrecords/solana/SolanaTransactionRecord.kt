package cash.p.terminal.entities.transactionrecords.solana

import cash.p.terminal.core.adapters.BaseSolanaAdapter
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.solanakit.models.Transaction

open class SolanaTransactionRecord(transaction: Transaction, baseToken: Token, source: TransactionSource, spam: Boolean = false) :
        TransactionRecord(
                uid = transaction.hash,
                transactionHash = transaction.hash,
                transactionIndex = 0,
                blockHeight = if (transaction.pending) null else 0,
                confirmationsThreshold = BaseSolanaAdapter.confirmationsThreshold,
                timestamp = transaction.timestamp,
                failed = transaction.error != null,
                spam = spam,
                source = source
        ) {

    data class Transfer(val address: String?, val value: TransactionValue)

    val fee: TransactionValue?

    init {
        fee = transaction.fee?.let { TransactionValue.CoinValue(baseToken, it) }
    }

}

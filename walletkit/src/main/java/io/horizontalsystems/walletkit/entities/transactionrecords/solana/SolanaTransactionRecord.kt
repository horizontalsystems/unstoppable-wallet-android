package io.horizontalsystems.walletkit.entities.transactionrecords.solana

import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

/** Kit-free essentials of a Solana transaction, extracted by the chain module's converter. */
data class SolanaTransactionInfo(
        val hash: String,
        val pending: Boolean,
        val timestamp: Long,
        val failed: Boolean,
        val fee: BigDecimal?,
)

open class SolanaTransactionRecord(transaction: SolanaTransactionInfo, baseToken: Token, source: TransactionSource, spam: Boolean = false) :
        TransactionRecord(
                uid = transaction.hash,
                transactionHash = transaction.hash,
                transactionIndex = 0,
                blockHeight = if (transaction.pending) null else 0,
                confirmationsThreshold = CONFIRMATIONS_THRESHOLD,
                timestamp = transaction.timestamp,
                failed = transaction.failed,
                spam = spam,
                source = source
        ) {

    data class Transfer(val address: String?, val value: TransactionValue)

    val fee: TransactionValue?

    init {
        fee = transaction.fee?.let { TransactionValue.CoinValue(baseToken, it) }
    }


    companion object {
        const val CONFIRMATIONS_THRESHOLD: Int = 12
    }
}

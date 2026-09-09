package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.walletkit.modules.transactions.TransactionStatus
import java.math.BigDecimal

/** XRP Ledger transaction as shown in the wallet. Kit-free so core can render it without the kit. */
class XrpTransactionRecord(
    baseToken: Token,
    source: TransactionSource,
    val info: XrpTransactionInfo,
    val type: Type,
    spam: Boolean,
) : TransactionRecord(
    uid = info.hash,
    transactionHash = info.hash,
    transactionIndex = 0,
    blockHeight = info.ledgerIndex?.toInt(),
    confirmationsThreshold = 1,
    timestamp = info.timestamp,
    failed = info.failed,
    spam = spam,
    source = source,
) {
    override val mainValue = type.mainValue
    val fee = TransactionValue.CoinValue(baseToken, info.fee)
    val memo = info.memo
    val destinationTag = info.destinationTag

    sealed class Type {
        data class Send(
            val value: TransactionValue,
            val to: String,
            val sentToSelf: Boolean,
            val comment: String?,
        ) : Type()

        data class Receive(
            val value: TransactionValue,
            val from: String,
            val comment: String?,
        ) : Type()

        /** A trust line created, changed or removed; [value] is the new limit. */
        data class TrustSet(
            val issuer: String,
            val value: TransactionValue,
        ) : Type()

        class Unsupported(val type: String) : Type()

        val mainValue: TransactionValue?
            get() = when (this) {
                is Receive -> value
                is Send -> value
                is TrustSet -> value
                is Unsupported -> null
            }
    }

    // A validated XRPL ledger is final, so there is no confirmation count: a transaction is
    // pending until the node reports it validated, then completed or failed for good.
    override fun status(lastBlockHeight: Int?): TransactionStatus = when {
        failed -> TransactionStatus.Failed
        info.validated -> TransactionStatus.Completed
        else -> TransactionStatus.Pending
    }

    companion object {
        fun eventsForPhishingCheck(type: Type): List<TransferEvent> =
            when (type) {
                is Type.Receive -> listOf(TransferEvent(type.from, type.value))

                is Type.Send,
                is Type.TrustSet,
                is Type.Unsupported -> listOf()
            }
    }
}

/** Kit-free projection of the xrpkit Transaction fields the record needs. */
data class XrpTransactionInfo(
    val hash: String,
    val ledgerIndex: Long?,
    val timestamp: Long,
    val validated: Boolean,
    val failed: Boolean,
    val fee: BigDecimal,
    val memo: String?,
    val destinationTag: Long?,
)

package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.walletkit.modules.transactions.TransactionStatus

/** NEAR transaction as shown in the wallet. Kit-free so core can render it without the kit. */
class NearTransactionRecord(
    source: TransactionSource,
    val info: NearTransactionInfo,
    val type: Type,
    /** Gas the account paid; null when someone else signed the transaction. */
    val fee: TransactionValue?,
    spam: Boolean,
) : TransactionRecord(
    uid = info.hash,
    transactionHash = info.hash,
    transactionIndex = 0,
    blockHeight = info.blockHeight?.toInt(),
    confirmationsThreshold = 1,
    timestamp = info.timestamp,
    failed = info.failed,
    spam = spam,
    source = source,
) {
    override val mainValue = type.mainValue

    sealed class Type {
        data class Send(
            val value: TransactionValue,
            val to: String,
            val sentToSelf: Boolean,
            val memo: String?,
        ) : Type()

        data class Receive(
            val value: TransactionValue,
            val from: String,
            val memo: String?,
        ) : Type()

        /**
         * Any other transaction, with the account's net movement per asset: none for a call that
         * moved nothing, several for a swap or a wrap.
         */
        data class ContractCall(
            val contractId: String,
            val method: String?,
            val incoming: List<TransferEvent>,
            val outgoing: List<TransferEvent>,
        ) : Type()

        val mainValue: TransactionValue?
            get() = when (this) {
                is Receive -> value
                is Send -> value
                is ContractCall -> (incoming + outgoing).singleOrNull()?.value
            }
    }

    // A final NEAR block cannot be reverted, so there is no confirmation count: a transaction
    // is pending until its receipts have executed, then completed or failed for good.
    override fun status(lastBlockHeight: Int?): TransactionStatus = when {
        failed -> TransactionStatus.Failed
        info.pending -> TransactionStatus.Pending
        else -> TransactionStatus.Completed
    }

    companion object {
        fun eventsForPhishingCheck(type: Type): List<TransferEvent> =
            when (type) {
                is Type.Receive -> listOf(TransferEvent(type.from, type.value))
                is Type.ContractCall -> type.incoming
                is Type.Send -> listOf()
            }
    }
}

/** Kit-free projection of the nearkit Transaction fields the record needs. */
data class NearTransactionInfo(
    val hash: String,
    val blockHeight: Long?,
    val timestamp: Long,
    val pending: Boolean,
    val failed: Boolean,
    /** Failed because it was never included before its block hash expired: nothing happened on chain. */
    val expired: Boolean,
)

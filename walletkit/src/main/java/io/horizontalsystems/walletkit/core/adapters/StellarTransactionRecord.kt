package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.walletkit.modules.transactions.TransactionStatus
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

class StellarTransactionRecord(
    baseToken: Token,
    source: TransactionSource,
    val operation: StellarOperationInfo,
    val type: Type,
    spam: Boolean,
) : TransactionRecord(
    uid = operation.id.toString(),
    transactionHash = operation.transactionHash,
    transactionIndex = 0,
    blockHeight = null,
    confirmationsThreshold = null,
    timestamp = operation.timestamp,
    failed = !operation.transactionSuccessful,
    spam = spam,
    source = source,
) {
    override val mainValue = type.mainValue
    val fee = operation.fee?.let { TransactionValue.CoinValue(baseToken, it) }
    val memo = operation.memo

    sealed class Type {
        data class Send(
            val value: TransactionValue,
            val to: String,
            val sentToSelf: Boolean,
            val comment: String?,
            val accountCreated: Boolean,
        ) : Type()

        data class Receive(
            val value: TransactionValue,
            val from: String,
            val comment: String?,
            val accountCreated: Boolean,
        ) : Type()

        data class ChangeTrust(
            val trustee: String,
            val value: TransactionValue
        ) : Type()

        // A swap on the own account: a path payment to self (Stellar DEX) or a contract
        // call that both spent and received assets (Soroswap/Aquarius). valueIn is what
        // was sold (negative), valueOut what was received.
        data class Swap(
            val valueIn: TransactionValue,
            val valueOut: TransactionValue,
        ) : Type()

        class Unsupported(val type: String) : Type()

        val mainValue: TransactionValue?
            get() = when (this) {
                is Receive -> value
                is Send -> value
                is ChangeTrust -> value
                is Swap -> valueOut
                is Unsupported -> null
            }
    }

    override fun status(lastBlockHeight: Int?) = if (failed) {
        TransactionStatus.Failed
    } else {
        TransactionStatus.Completed
    }

    companion object {
        fun eventsForPhishingCheck(type: Type): List<TransferEvent> =
            when (type) {
                is Type.Receive -> {
                    listOf(TransferEvent(type.from, type.value))
                }

                is Type.ChangeTrust,
                is Type.Send,
                is Type.Swap,
                is Type.Unsupported -> listOf()
            }
    }
}
/** Kit-free projection of the stellar-kit Operation fields the record needs. */
data class StellarOperationInfo(
    val id: Long,
    val transactionHash: String,
    val timestamp: Long,
    val transactionSuccessful: Boolean,
    val fee: BigDecimal?,
    val memo: String?,
)

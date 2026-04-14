package com.quantum.wallet.bankwallet.core.adapters

import com.quantum.wallet.bankwallet.entities.TransactionValue
import com.quantum.wallet.bankwallet.entities.transactionrecords.TransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.TransferEvent
import com.quantum.wallet.bankwallet.modules.transactions.TransactionSource
import com.quantum.wallet.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.stellarkit.room.Operation

class StellarTransactionRecord(
    baseToken: Token,
    source: TransactionSource,
    val operation: Operation,
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

        class Unsupported(val type: String) : Type()

        val mainValue: TransactionValue?
            get() = when (this) {
                is Receive -> value
                is Send -> value
                is ChangeTrust -> value
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
                is Type.Unsupported -> listOf()
            }
    }
}
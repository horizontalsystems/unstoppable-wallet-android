package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.stellarkit.room.Operation

class StellarTransactionRecord(
    baseToken: Token,
    source: TransactionSource,
    val operation: Operation,
    val type: Type,
) : TransactionRecord(
    uid = operation.id.toString(),
    transactionHash = operation.transactionHash,
    transactionIndex = 0,
    blockHeight = null,
    confirmationsThreshold = null,
    timestamp = operation.timestamp,
    failed = !operation.transactionSuccessful,
    spam = false,
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
        ) : Type()

        data class Receive(
            val value: TransactionValue,
            val from: String,
            val comment: String?,
        ) : Type()

        data class AccountCreated(
            val funder: String,
            val account: String,
            val value: TransactionValue.CoinValue
        ) : Type()

        data class ChangeTrust(
            val outgoing: Boolean,
            val sentToSelf: Boolean,
            val token: Token,
            val trustee: String,
        ) : Type()

        object Unsupported: Type()

        val mainValue: TransactionValue?
            get() = when (this) {
                is AccountCreated -> value
                is Receive -> value
                is Send -> value
                is ChangeTrust -> null
                Unsupported -> null
            }
    }

    override fun status(lastBlockHeight: Int?) = if (failed) {
        TransactionStatus.Failed
    } else {
        TransactionStatus.Completed
    }
}
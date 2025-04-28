package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.stellarkit.room.Operation

class StellarTransactionRecord(
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

        object Unsupported: Type()
    }
}
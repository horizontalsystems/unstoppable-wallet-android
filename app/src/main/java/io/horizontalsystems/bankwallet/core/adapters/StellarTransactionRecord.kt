package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.stellarkit.room.Event

class StellarTransactionRecord(
    source: TransactionSource,
    val operation: Event,
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
)

package io.horizontalsystems.walletkit.entities.transactionrecords.thorchain

import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource

abstract class ThorchainTransactionRecord(
    uid: String,
    transactionHash: String,
    blockHeight: Int?,
    timestamp: Long,
    val fee: TransactionValue?,
    failed: Boolean,
    val memo: String?,
    source: TransactionSource
) : TransactionRecord(
    uid = uid,
    transactionHash = transactionHash,
    transactionIndex = 0,
    blockHeight = blockHeight,
    confirmationsThreshold = 1,
    timestamp = timestamp,
    failed = failed,
    source = source
)

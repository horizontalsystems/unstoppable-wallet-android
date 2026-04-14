package com.quantum.wallet.bankwallet.entities.transactionrecords.monero

import com.quantum.wallet.bankwallet.entities.TransactionValue
import com.quantum.wallet.bankwallet.entities.transactionrecords.TransactionRecord
import com.quantum.wallet.bankwallet.modules.transactions.TransactionSource

abstract class MoneroTransactionRecord(
    uid: String,
    transactionHash: String,
    transactionIndex: Int,
    blockHeight: Int?,
    confirmationsThreshold: Int?,
    timestamp: Long,
    val fee: TransactionValue?,
    failed: Boolean,
    val memo: String?,
    source: TransactionSource
) : TransactionRecord(
    uid = uid,
    transactionHash = transactionHash,
    transactionIndex = transactionIndex,
    blockHeight = blockHeight,
    confirmationsThreshold = confirmationsThreshold,
    timestamp = timestamp,
    failed = failed,
    source = source
)

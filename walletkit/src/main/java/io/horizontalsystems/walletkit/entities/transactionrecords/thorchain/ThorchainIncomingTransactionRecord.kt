package io.horizontalsystems.walletkit.entities.transactionrecords.thorchain

import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource

class ThorchainIncomingTransactionRecord(
    uid: String,
    transactionHash: String,
    blockHeight: Int?,
    timestamp: Long,
    fee: TransactionValue?,
    failed: Boolean,
    val value: TransactionValue,
    val from: String?,
    memo: String?,
    source: TransactionSource
) : ThorchainTransactionRecord(
    uid = uid,
    transactionHash = transactionHash,
    blockHeight = blockHeight,
    timestamp = timestamp,
    fee = fee,
    failed = failed,
    memo = memo,
    source = source
) {
    override val mainValue = value
}

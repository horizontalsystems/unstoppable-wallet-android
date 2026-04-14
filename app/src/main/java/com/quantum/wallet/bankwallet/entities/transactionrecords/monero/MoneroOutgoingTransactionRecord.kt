package com.quantum.wallet.bankwallet.entities.transactionrecords.monero

import com.quantum.wallet.bankwallet.entities.TransactionValue
import com.quantum.wallet.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

class MoneroOutgoingTransactionRecord(
    token: Token,
    uid: String,
    transactionHash: String,
    transactionIndex: Int,
    blockHeight: Int?,
    confirmationsThreshold: Int?,
    timestamp: Long,
    fee: BigDecimal?,
    failed: Boolean,
    amount: BigDecimal,
    val to: String?,
    val sentToSelf: Boolean,
    memo: String?,
    source: TransactionSource,
    val txKey: String?
) : MoneroTransactionRecord(
    uid = uid,
    transactionHash = transactionHash,
    transactionIndex = transactionIndex,
    blockHeight = blockHeight,
    confirmationsThreshold = confirmationsThreshold,
    timestamp = timestamp,
    fee = fee?.let { TransactionValue.CoinValue(token, it) },
    failed = failed,
    memo = memo,
    source = source
) {
    val value: TransactionValue = TransactionValue.CoinValue(token, amount)

    override val mainValue = value
}

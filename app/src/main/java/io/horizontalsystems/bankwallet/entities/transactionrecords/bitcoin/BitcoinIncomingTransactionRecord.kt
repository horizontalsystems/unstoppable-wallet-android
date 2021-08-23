package io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionLockInfo
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.coinkit.models.Coin
import java.math.BigDecimal

class BitcoinIncomingTransactionRecord(
    coin: Coin,
    uid: String,
    transactionHash: String,
    transactionIndex: Int,
    blockHeight: Int?,
    confirmationsThreshold: Int?,
    timestamp: Long,
    fee: BigDecimal?,
    failed: Boolean,
    lockInfo: TransactionLockInfo?,
    conflictingHash: String?,
    showRawTransaction: Boolean,
    amount: BigDecimal,
    val from: String?,
    memo: String? = null,
    source: TransactionSource
) : BitcoinTransactionRecord(
    uid = uid,
    transactionHash = transactionHash,
    transactionIndex = transactionIndex,
    blockHeight = blockHeight,
    confirmationsThreshold = confirmationsThreshold,
    timestamp = timestamp,
    fee = fee?.let { CoinValue(coin, it) },
    failed = failed,
    lockInfo = lockInfo,
    conflictingHash = conflictingHash,
    showRawTransaction = showRawTransaction,
    memo = memo,
    source = source
) {

    val value: CoinValue = CoinValue(coin, amount)

    override val mainValue: CoinValue = value

}

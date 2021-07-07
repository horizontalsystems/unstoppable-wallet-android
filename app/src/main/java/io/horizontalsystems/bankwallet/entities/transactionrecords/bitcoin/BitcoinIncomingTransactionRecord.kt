package io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.modules.transactions.TransactionLockInfo
import io.horizontalsystems.bankwallet.modules.transactions.TransactionType
import io.horizontalsystems.coinkit.models.Coin
import java.math.BigDecimal
import java.util.*

class BitcoinIncomingTransactionRecord(
    coin: Coin,
    uid: String,
    transactionHash: String,
    transactionIndex: Int,
    blockHeight: Int?,
    confirmationsThreshold: Int?,
    date: Date,
    fee: BigDecimal?,
    failed: Boolean,
    lockInfo: TransactionLockInfo?,
    conflictingHash: String?,
    showRawTransaction: Boolean,
    amount: BigDecimal,
    val from: String?
) : BitcoinTransactionRecord(
    uid = uid,
    transactionHash = transactionHash,
    transactionIndex = transactionIndex,
    blockHeight = blockHeight,
    confirmationsThreshold = confirmationsThreshold,
    date = date,
    fee = fee?.let { CoinValue(coin, it) },
    failed = failed,
    lockInfo = lockInfo,
    conflictingHash = conflictingHash,
    showRawTransaction = showRawTransaction
) {

    val value: CoinValue = CoinValue(coin, amount)

    override val mainValue: CoinValue = value

    override fun getType(lastBlockInfo: LastBlockInfo?): TransactionType {
        val lockState = lockState(lastBlockInfo?.timestamp)
        return TransactionType.Incoming(from, value, lockState, conflictingHash)
    }
}

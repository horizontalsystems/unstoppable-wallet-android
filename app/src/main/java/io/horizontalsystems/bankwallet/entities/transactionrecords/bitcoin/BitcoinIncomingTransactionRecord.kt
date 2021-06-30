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
        val amount: BigDecimal,
        val from: String?
) : BitcoinTransactionRecord(
        coin = coin,
        uid = uid,
        transactionHash = transactionHash,
        transactionIndex = transactionIndex,
        blockHeight = blockHeight,
        confirmationsThreshold = confirmationsThreshold,
        date = date,
        fee = fee,
        failed = failed,
        lockInfo = lockInfo,
        conflictingHash = conflictingHash,
        showRawTransaction = showRawTransaction
) {

    override val mainAmount: BigDecimal?
        get() = amount

    override fun getType(lastBlockInfo: LastBlockInfo?): TransactionType {
        val coinValue = CoinValue(coin, amount)
        val lockState = lockState(lastBlockInfo?.timestamp)
        return TransactionType.Incoming(from, coinValue, lockState, conflictingHash)
    }
}

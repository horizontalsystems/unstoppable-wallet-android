package io.horizontalsystems.bankwallet.entities.transactionrecords.tron

import io.horizontalsystems.bankwallet.core.adapters.BaseTronAdapter
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.tronkit.models.Transaction

open class TronTransactionRecord(
    transaction: Transaction,
    baseToken: Token,
    source: TransactionSource,
    val foreignTransaction: Boolean = false,
    spam: Boolean = false
) :
    TransactionRecord(
        uid = transaction.hashString,
        transactionHash = transaction.hashString,
        transactionIndex = 0,
        blockHeight = transaction.blockNumber?.toInt(),
        confirmationsThreshold = BaseTronAdapter.confirmationsThreshold,
        timestamp = transaction.timestamp / 1000,
        failed = transaction.isFailed,
        spam = spam,
        source = source
    ) {

    val fee: TransactionValue?

    init {
        val feeAmount: Long? = transaction.fee
        fee = if (feeAmount != null) {
            val feeDecimal = feeAmount.toBigDecimal()
                .movePointLeft(baseToken.decimals).stripTrailingZeros()

            TransactionValue.CoinValue(baseToken, feeDecimal)
        } else {
            null
        }
    }

}

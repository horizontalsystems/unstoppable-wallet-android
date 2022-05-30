package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.core.adapters.BaseEvmAdapter
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.marketkit.models.PlatformCoin

open class EvmTransactionRecord(transaction: Transaction, baseCoin: PlatformCoin, source: TransactionSource, val foreignTransaction: Boolean = false, spam: Boolean = false) :
    TransactionRecord(
        uid = transaction.hashString,
        transactionHash = transaction.hashString,
        transactionIndex = transaction.transactionIndex ?: 0,
        blockHeight = transaction.blockNumber?.toInt(),
        confirmationsThreshold = BaseEvmAdapter.confirmationsThreshold,
        timestamp = transaction.timestamp,
        failed = transaction.isFailed,
        spam = spam,
        source = source
    ) {

    data class TransferEvent(val address: String?, val value: TransactionValue)

    val fee: TransactionValue?

    init {
        val feeAmount: Long? = transaction.gasUsed ?: transaction.gasLimit
        val gasPrice = transaction.gasPrice

        fee = if (feeAmount != null && gasPrice != null) {
            val feeDecimal = feeAmount.toBigDecimal()
                .multiply(gasPrice.toBigDecimal())
                .movePointLeft(baseCoin.decimals).stripTrailingZeros()

            TransactionValue.CoinValue(baseCoin, feeDecimal)
        } else {
            null
        }
    }

}

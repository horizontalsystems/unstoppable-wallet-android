package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.core.adapters.BaseEvmAdapter
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.marketkit.models.Token

open class EvmTransactionRecord(transaction: Transaction, baseToken: Token, source: TransactionSource, val foreignTransaction: Boolean = false, spam: Boolean = false) :
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
                .movePointLeft(baseToken.decimals).stripTrailingZeros()

            TransactionValue.CoinValue(baseToken, feeDecimal)
        } else {
            null
        }
    }

}

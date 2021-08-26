package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.core.adapters.BaseEvmAdapter
import io.horizontalsystems.bankwallet.core.adapters.EvmAdapter
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.toHexString
import io.horizontalsystems.ethereumkit.models.FullTransaction

abstract class EvmTransactionRecord(fullTransaction: FullTransaction, baseCoin: Coin, source: TransactionSource) :
    TransactionRecord(
        uid = fullTransaction.transaction.hash.toHexString(),
        transactionHash = fullTransaction.transaction.hash.toHexString(),
        transactionIndex = fullTransaction.receiptWithLogs?.receipt?.transactionIndex ?: 0,
        blockHeight = fullTransaction.receiptWithLogs?.receipt?.blockNumber?.toInt(),
        confirmationsThreshold = BaseEvmAdapter.confirmationsThreshold,
        timestamp = fullTransaction.transaction.timestamp,
        failed = fullTransaction.isFailed(),
        source = source
    ) {

    val fee: CoinValue
    open val foreignTransaction: Boolean = false

    init {
        val feeAmount: Long = fullTransaction.receiptWithLogs?.receipt?.gasUsed
            ?: fullTransaction.transaction.gasLimit

        val feeDecimal = feeAmount.toBigDecimal()
            .multiply(fullTransaction.transaction.gasPrice.toBigDecimal())
            .movePointLeft(EvmAdapter.decimal).stripTrailingZeros()

        fee = CoinValue(baseCoin, feeDecimal)
    }

}

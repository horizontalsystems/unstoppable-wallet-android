package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.core.adapters.BaseEvmAdapter
import io.horizontalsystems.bankwallet.core.adapters.EvmAdapter
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.core.toHexString
import io.horizontalsystems.ethereumkit.models.FullTransaction
import java.math.BigDecimal

abstract class EvmTransactionRecord(fullTransaction: FullTransaction) : TransactionRecord(
        uid = fullTransaction.transaction.hash.toHexString(),
        transactionHash = fullTransaction.transaction.hash.toHexString(),
        transactionIndex = fullTransaction.receiptWithLogs?.receipt?.transactionIndex ?: 0,
        blockHeight = fullTransaction.receiptWithLogs?.receipt?.blockNumber?.toInt(),
        confirmationsThreshold = BaseEvmAdapter.confirmationsThreshold,
        fee = fullTransaction.receiptWithLogs?.receipt?.gasUsed
                ?.toBigDecimal()
                ?.multiply(fullTransaction.transaction.gasPrice.toBigDecimal())
                ?.movePointLeft(EvmAdapter.decimal)
                ?.stripTrailingZeros(),
        timestamp = fullTransaction.transaction.timestamp,
        failed = fullTransaction.isFailed()
) {

    var incomingInternalETHs = mutableListOf<Pair<String, BigDecimal>>()
    var incomingEip20Events = mutableListOf<Pair<String, BigDecimal>>()
    var outgoingEip20Events = mutableListOf<Pair<String, BigDecimal>>()

}

package io.horizontalsystems.walletkit.entities.transactionrecords.tron

import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.entities.transactionrecords.evm.EvmTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token

class TronContractCallTransactionRecord(
    transaction: TronTransactionInfo,
    baseToken: Token,
    source: TransactionSource,
    val contractAddress: String,
    val method: String?,
    val incomingEvents: List<TransferEvent>,
    val outgoingEvents: List<TransferEvent>
) : TronTransactionRecord(transaction, baseToken, source) {

    override val mainValue: TransactionValue?
        get() {
            val (incomingValues, outgoingValues) = EvmTransactionRecord.combined(incomingEvents, outgoingEvents)

            return when {
                (incomingValues.isEmpty() && outgoingValues.size == 1) -> outgoingValues.first()
                (incomingValues.size == 1 && outgoingValues.isEmpty()) -> incomingValues.first()
                else -> null
            }
        }

}

package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.marketkit.models.Token

class ContractCallTransactionRecord(
    transaction: Transaction,
    baseToken: Token,
    source: TransactionSource,
    val contractAddress: String,
    val method: String?,
    val incomingEvents: List<TransferEvent>,
    val outgoingEvents: List<TransferEvent>
) : EvmTransactionRecord(transaction, baseToken, source) {

    override val mainValue: TransactionValue? =
        when {
            (incomingEvents.isEmpty() && outgoingEvents.size == 1) -> outgoingEvents.first().value
            (incomingEvents.size == 1 && outgoingEvents.isEmpty()) -> incomingEvents.first().value
            else -> null
        }

}

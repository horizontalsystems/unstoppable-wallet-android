package cash.p.terminal.entities.transactionrecords.evm

import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.modules.transactions.TransactionSource
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

    override val mainValue: TransactionValue?
        get() {
            val (incomingValues, outgoingValues) = combined(incomingEvents, outgoingEvents)

            return when {
                (incomingValues.isEmpty() && outgoingValues.size == 1) -> outgoingValues.first()
                (incomingValues.size == 1 && outgoingValues.isEmpty()) -> incomingValues.first()
                else -> null
            }
        }

}
